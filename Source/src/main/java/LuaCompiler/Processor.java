package LuaCompiler;

/**
 * Created by lijin on 5/22/19.
 */
import AST.Block;
import AST.Exp;
import AST.Exps.*;
import AST.Stat;
import AST.Stats.*;
import Lexer.TokenType;
import Operation.OprEnum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static LuaCompiler.ExpProcessor.ARG_REG;

public class Processor {
    static boolean isVarargOrFuncCall(Exp exp) {
        return exp instanceof VarargExp
                || exp instanceof FuncCallExp;
    }

    static List<Exp> removeTailNils(List<Exp> exps) {
        while (!exps.isEmpty()) {
            if (exps.get(exps.size() - 1) instanceof NilExp) {
                exps.remove(exps.size() - 1);
            } else {
                break;
            }
        }
        return exps;
    }
}

class ExpProcessor extends Processor{
    // kind of operands
    static final int ARG_CONST = 1; // const index
    static final int ARG_REG   = 2; // register index
    static final int ARG_UPVAL = 4; // upvalue index
    static final int ARG_RK    = ARG_REG | ARG_CONST;
    static final int ARG_RU    = ARG_REG | ARG_UPVAL;
    //static final int ARG_RUK   = ARG_REG | ARG_UPVAL | ARG_CONST;

    static class ArgAndKind {
        int arg;
        int kind;
    }


    static void processExp(FuncInformation fi, Exp node, int a, int n) {
        if (node instanceof NilExp) {
            fi.emitLoadNil(a, n);
        } else if (node instanceof FalseExp) {
            fi.emitLoadBool(  a, 0, 0);
        } else if (node instanceof TrueExp) {
            fi.emitLoadBool(  a, 1, 0);
        } else if (node instanceof IntExp) {
            fi.emitLoadK(  a, ((IntExp) node).val);
        } else if (node instanceof FloatExp) {
            fi.emitLoadK(  a, ((FloatExp) node).val);
        } else if (node instanceof StringExp) {
            fi.emitLoadK(  a, ((StringExp) node).str);
        } else if (node instanceof ParentExp) {
            processExp(fi, ((ParentExp) node).exp, a, 1);
        } else if (node instanceof VarargExp) {
            processVarargExp(fi, (VarargExp) node, a, n);
        } else if (node instanceof FuncDefExp) {
            processFuncDefExp(fi, (FuncDefExp) node, a);
        } else if (node instanceof TableBuilderExp) {
            processTableConstructorExp(fi, (TableBuilderExp) node, a);
        } else if (node instanceof UnaryOpExp) {
            processUnopExp(fi, (UnaryOpExp) node, a);
        } else if (node instanceof BinaryOpExp) {
            processBinopExp(fi, (BinaryOpExp) node, a);
        } else if (node instanceof ConcatExp) {
            processConcatExp(fi, (ConcatExp) node, a);
        } else if (node instanceof LabelExp) {
            processNameExp(fi, (LabelExp) node, a);
        } else if (node instanceof TableVisitExp) {
            processTableAccessExp(fi, (TableVisitExp) node, a);
        } else if (node instanceof FuncCallExp) {
            processFuncCallExp(fi, (FuncCallExp) node, a, n);
        }
    }

    private static void processVarargExp(FuncInformation fi, VarargExp node, int a, int n) {
        if (!fi.isVararg) {
            throw new RuntimeException("cannot use '...' outside a vararg function");
        }
        fi.emitVararg(  a, n);
    }

    // f[a] = function(args) body end
    static void processFuncDefExp(FuncInformation fi, FuncDefExp node, int a) {
        FuncInformation subFI = new FuncInformation(fi, node);
        fi.subFuncs.add(subFI);

        if (node.parList != null) {
            for (String param : node.parList) {
                subFI.addLocVar(param, 0);
            }
        }

        BlockProcessor.processBlock(subFI, node.block);
        subFI.exitScope(subFI.pc() + 2);
        subFI.emitReturn(  0, 0);

        int bx = fi.subFuncs.size() - 1;
        fi.emitClosure(  a, bx);
    }

    private static void processTableConstructorExp(FuncInformation fi, TableBuilderExp node, int a) {
        int nArr = 0;
        for (Exp keyExp : node.keyExps) {
            if (keyExp == null) {
                nArr++;
            }
        }
        int nExps = node.keyExps.size();
        boolean multRet = nExps > 0 &&
                isVarargOrFuncCall(node.valExps.get(nExps-1));

        fi.emitNewTable(  a, nArr, nExps-nArr);

        int arrIdx = 0;
        for (int i = 0; i < node.keyExps.size(); i++) {
            Exp keyExp = node.keyExps.get(i);
            Exp valExp = node.valExps.get(i);

            if (keyExp == null) {
                arrIdx++;
                int tmp = fi.allocReg();
                if (i == nExps-1 && multRet) {
                    processExp(fi, valExp, tmp, -1);
                } else {
                    processExp(fi, valExp, tmp, 1);
                }

                if (arrIdx%50 == 0 || arrIdx == nArr) { // LFIELDS_PER_FLUSH
                    int n = arrIdx % 50;
                    if (n == 0) {
                        n = 50;
                    }
                    fi.freeRegs(n);
                    int c = (arrIdx-1)/50 + 1; // todo: c > 0xFF
                    if (i == nExps-1 && multRet) {
                        fi.emitSetList(  a, 0, c);
                    } else {
                        fi.emitSetList(  a, n, c);
                    }
                }

                continue;
            }

            int b = fi.allocReg();
            processExp(fi, keyExp, b, 1);
            int c = fi.allocReg();
            processExp(fi, valExp, c, 1);
            fi.freeRegs(2);

            fi.emitSetTable(  a, b, c);
        }
    }

    // r[a] = op exp
    private static void processUnopExp(FuncInformation fi, UnaryOpExp node, int a) {
        int oldRegs = fi.usedRegs;
        int b = expToOpArg(fi, node.exp, ARG_REG).arg;
        fi.emitUnaryOp(  node.op, a, b);
        fi.usedRegs = oldRegs;
    }

    // r[a] = exp1 op exp2
    private static void processBinopExp(FuncInformation fi, BinaryOpExp node, int a) {
        if (node.op == TokenType.OP_AND || node.op == TokenType.OP_OR) {
            int oldRegs = fi.usedRegs;

            int b = expToOpArg(fi, node.exp1, ARG_REG).arg;
            fi.usedRegs = oldRegs;
            if (node.op == TokenType.OP_AND) {
                // TODO
                fi.emitTestSet(  a, b, 0);
            } else {
                // TODO
                fi.emitTestSet(  a, b, 1);
            }
            int pcOfJmp = fi.emitJmp(  0, 0);

            b = expToOpArg(fi, node.exp2, ARG_REG).arg;
            fi.usedRegs = oldRegs;
            fi.emitMove(  a, b);
            fi.fixSbx(pcOfJmp, fi.pc()-pcOfJmp);
        } else {
            int oldRegs = fi.usedRegs;
            int b = expToOpArg(fi, node.exp1, ARG_RK).arg;
            int c = expToOpArg(fi, node.exp2, ARG_RK).arg;
            fi.emitBinaryOp(  node.op, a, b, c);
            fi.usedRegs = oldRegs;
        }
    }

    // r[a] = exp1 .. exp2
    private static void processConcatExp(FuncInformation fi, ConcatExp node, int a) {
        for (Exp subExp : node.exps) {
            int a1 = fi.allocReg();
            processExp(fi, subExp, a1, 1);
        }

        int c = fi.usedRegs - 1;
        int b = c - node.exps.size() + 1;
        fi.freeRegs(c - b + 1);
        fi.emitABC(  OprEnum.CONCAT, a, b, c);
    }

    // r[a] = name
    private static void processNameExp(FuncInformation fi, LabelExp node, int a) {
        int r = fi.slotOfLocVar(node.name);
        if (r >= 0) {
            fi.emitMove(  a, r);
            return;
        }

        int idx = fi.indexOfUpval(node.name);
        if (idx >= 0) {
            fi.emitGetUpval(  a, idx);
            return;
        }

        // x => _ENV['x']
        Exp prefixExp = new LabelExp("_ENV");
        Exp keyExp = new StringExp(node.name);
        TableVisitExp taExp = new TableVisitExp(prefixExp, keyExp);
        processTableAccessExp(fi, taExp, a);
    }

    // r[a] = prefix[key]
    private static void processTableAccessExp(FuncInformation fi, TableVisitExp node, int a) {
        int oldRegs = fi.usedRegs;
        ArgAndKind argAndKindB = expToOpArg(fi, node.prefixExp, ARG_RU);
        int b = argAndKindB.arg;
        int c = expToOpArg(fi, node.varExp, ARG_RK).arg;
        fi.usedRegs = oldRegs;

        if (argAndKindB.kind == ARG_UPVAL) {
            fi.emitGetTabUp(  a, b, c);
        } else {
            fi.emitGetTable(  a, b, c);
        }
    }

    // r[a] = f(args)
    static void processFuncCallExp(FuncInformation fi, FuncCallExp node, int a, int n) {
        int nArgs = prepFuncCall(fi, node, a);
        fi.emitCall(  a, nArgs, n);
    }

    // return f(args)
    static void processTailCallExp(FuncInformation fi, FuncCallExp node, int a) {
        int nArgs = prepFuncCall(fi, node, a);
        fi.emitTailCall(  a, nArgs);
    }

    private static int prepFuncCall(FuncInformation fi, FuncCallExp node, int a) {
        List<Exp> args = node.args;
        if (args == null) {
            args = Collections.emptyList();
        }
        int nArgs = args.size();
        boolean lastArgIsVarargOrFuncCall = false;

        processExp(fi, node.prefixExp, a, 1);
        if (node.nameExp != null) {
            fi.allocReg();
            ArgAndKind argAndKindC = expToOpArg(fi, node.nameExp, ARG_RK);
            fi.emitSelf(  a, a, argAndKindC.arg);
            if (argAndKindC.kind == ARG_REG) {
                fi.freeRegs(1);
            }
        }
        for (int i = 0; i < args.size(); i++) {
            Exp arg = args.get(i);
            int tmp = fi.allocReg();
            if (i == nArgs-1 && isVarargOrFuncCall(arg)) {
                lastArgIsVarargOrFuncCall = true;
                processExp(fi, arg, tmp, -1);
            } else {
                processExp(fi, arg, tmp, 1);
            }
        }
        fi.freeRegs(nArgs);

        if (node.nameExp != null) {
            fi.freeReg();
            nArgs++;
        }
        if (lastArgIsVarargOrFuncCall) {
            nArgs = -1;
        }

        return nArgs;
    }

    static ArgAndKind expToOpArg(FuncInformation fi, Exp node, int argKinds) {
        ArgAndKind ak = new ArgAndKind();

        if ((argKinds & ARG_CONST) > 0) {
            int idx = -1;
            if (node instanceof NilExp) {
                idx = fi.indexOfConstant(null);
            } else if (node instanceof FalseExp) {
                idx = fi.indexOfConstant(false);
            } else if (node instanceof TrueExp) {
                idx = fi.indexOfConstant(true);
            } else if (node instanceof IntExp) {
                idx = fi.indexOfConstant(((IntExp) node).val);
            } else if (node instanceof FloatExp) {
                idx = fi.indexOfConstant(((FloatExp) node).val);
            } else if (node instanceof StringExp) {
                idx = fi.indexOfConstant(((StringExp) node).str);
            }
            if (idx >= 0 && idx <= 0xFF) {
                ak.arg = 0x100 + idx;
                ak.kind = ARG_CONST;
                return ak;
            }
        }

        if (node instanceof LabelExp) {
            if ((argKinds & ARG_REG) > 0) {
                int r = fi.slotOfLocVar(((LabelExp) node).name);
                if (r >= 0) {
                    ak.arg = r;
                    ak.kind = ARG_REG;
                    return ak;
                }
            }
            if ((argKinds & ARG_UPVAL) > 0) {
                int idx = fi.indexOfUpval(((LabelExp) node).name);
                if (idx >= 0) {
                    ak.arg = idx;
                    ak.kind = ARG_UPVAL;
                    return ak;
                }
            }
        }

        int a = fi.allocReg();
        processExp(fi, node, a, 1);
        ak.arg = a;
        ak.kind = ARG_REG;
        return ak;
    }
}

class StatProcessor extends Processor{
    static void processStat(FuncInformation fi, Stat node) {
        if (node instanceof FuncCallStat) {
            processFuncCallStat(fi, (FuncCallStat) node);
        } else if (node instanceof BreakStat) {
            processBreakStat(fi, (BreakStat) node);
        } else if (node instanceof DoStat) {
            processDoStat(fi, (DoStat) node);
        } else if (node instanceof WhileStat) {
            processWhileStat(fi, (WhileStat) node);
        } else if (node instanceof RepeatStat) {
            processRepeatStat(fi, (RepeatStat) node);
        } else if (node instanceof IfStat) {
            processIfStat(fi, (IfStat) node);
        } else if (node instanceof ForNumStat) {
            processForNumStat(fi, (ForNumStat) node);
        } else if (node instanceof ForInStat) {
            processForInStat(fi, (ForInStat) node);
        } else if (node instanceof AssignStat) {
            processAssignStat(fi, (AssignStat) node);
        } else if (node instanceof LocalVarStat) {
            processLocalVarDeclStat(fi, (LocalVarStat) node);
        } else if (node instanceof LocalFuncStat) {
            processLocalFuncDefStat(fi, (LocalFuncStat) node);
        } else if (node instanceof LabelStat
                || node instanceof GotoStat) {
            throw new RuntimeException("label and goto statements are not supported!");
        }
    }

    private static void processLocalFuncDefStat(FuncInformation fi, LocalFuncStat node) {
        int r = fi.addLocVar(node.name, fi.pc()+2);
        ExpProcessor.processFuncDefExp(fi, node.exp, r);
    }

    private static void processFuncCallStat(FuncInformation fi, FuncCallStat node) {
        int r = fi.allocReg();
        ExpProcessor.processFuncCallExp(fi, node.exp, r, 0);
        fi.freeReg();
    }

    private static void processBreakStat(FuncInformation fi, BreakStat node) {
        int pc = fi.emitJmp(0, 0);
        fi.addBreakJmp(pc);
    }

    private static void processDoStat(FuncInformation fi, DoStat node) {
        fi.enterScope(false);
        BlockProcessor.processBlock(fi, node.block);
        fi.closeOpenUpvals( );
        fi.exitScope(fi.pc() + 1);
    }

    /*
               ______________
              /  false? jmp  |
             /               |
    while exp do block end <-'
          ^           \
          |___________/
               jmp
    */
    private static void processWhileStat(FuncInformation fi, WhileStat node) {
        int pcBeforeExp = fi.pc();

        int oldRegs = fi.usedRegs;
        int a = ExpProcessor.expToOpArg(fi, node.exp, ARG_REG).arg;
        fi.usedRegs = oldRegs;

        fi.emitTest(  a, 0);

        int pcJmpToEnd = fi.emitJmp(  0, 0);

        fi.enterScope(true);
        BlockProcessor.processBlock(fi, node.block);
        fi.closeOpenUpvals( );
        fi.emitJmp(0, pcBeforeExp-fi.pc()-1);
        fi.exitScope(fi.pc());

        fi.fixSbx(pcJmpToEnd, fi.pc()-pcJmpToEnd);
    }

    /*
            ______________
           |  false? jmp  |
           V              /
    repeat block until exp
    */
    private static void processRepeatStat(FuncInformation fi, RepeatStat node) {
        fi.enterScope(true);

        int pcBeforeBlock = fi.pc();
        BlockProcessor.processBlock(fi, node.block);

        int oldRegs = fi.usedRegs;
        int a = ExpProcessor.expToOpArg(fi, node.exp, ARG_REG).arg;
        fi.usedRegs = oldRegs;

        fi.emitTest(  a, 0);
        fi.emitJmp(  fi.getJmpArgA(), pcBeforeBlock-fi.pc()-1);

        fi.exitScope(fi.pc() + 1);
    }

    /*
             _________________       _________________       _____________
            / false? jmp      |     / false? jmp      |     / false? jmp  |
           /                  V    /                  V    /              V
    if exp1 then block1 elseif exp2 then block2 elseif true then block3 end <-.
                       \                       \                       \      |
                        \_______________________\_______________________\_____|
                        jmp                     jmp                     jmp
    */
    private static void processIfStat(FuncInformation fi, IfStat node) {
        int[] pcJmpToEnds = new int[node.exps.size()];
        int pcJmpToNextExp = -1;

        for (int i = 0; i < node.exps.size(); i++) {
            Exp exp = node.exps.get(i);
            if (pcJmpToNextExp >= 0) {
                fi.fixSbx(pcJmpToNextExp, fi.pc()-pcJmpToNextExp);
            }

            int oldRegs = fi.usedRegs;
            int a = ExpProcessor.expToOpArg(fi, exp, ARG_REG).arg;
            fi.usedRegs = oldRegs;
            
            fi.emitTest(  a, 0);
            pcJmpToNextExp = fi.emitJmp(  0, 0);

            Block block = node.blocks.get(i);
            fi.enterScope(false);
            BlockProcessor.processBlock(fi, block);
            fi.closeOpenUpvals();
            fi.exitScope(fi.pc() + 1);
            if (i < node.exps.size()-1) {
                pcJmpToEnds[i] = fi.emitJmp(0, 0);
            } else {
                pcJmpToEnds[i] = pcJmpToNextExp;
            }
        }

        for (int pc : pcJmpToEnds) {
            fi.fixSbx(pc, fi.pc()-pc);
        }
    }

    private static void processForNumStat(FuncInformation fi, ForNumStat node) {
        String forIndexVar = "(for index)";
        String forLimitVar = "(for limit)";
        String forStepVar = "(for step)";

        fi.enterScope(true);

        LocalVarStat lvdStat = new LocalVarStat(
                Arrays.asList(forIndexVar, forLimitVar, forStepVar),
                Arrays.asList(node.initExp, node.controllExp, node.modifyExp));
        processLocalVarDeclStat(fi, lvdStat);
        fi.addLocVar(node.var, fi.pc()+2);

        int a = fi.usedRegs - 4;
        int pcForPrep = fi.emitForPrep( a, 0);
        BlockProcessor.processBlock(fi, node.block);
        fi.closeOpenUpvals( );
        int pcForLoop = fi.emitForLoop(a, 0);

        fi.fixSbx(pcForPrep, pcForLoop-pcForPrep-1);
        fi.fixSbx(pcForLoop, pcForPrep-pcForLoop);

        fi.exitScope(fi.pc());
        fi.fixEndPC(forIndexVar, 1);
        fi.fixEndPC(forLimitVar, 1);
        fi.fixEndPC(forStepVar, 1);
    }

    private static void processForInStat(FuncInformation fi, ForInStat node) {
        String forGeneratorVar = "(for generator)";
        String forStateVar = "(for state)";
        String forControlVar = "(for control)";

        fi.enterScope(true);

        LocalVarStat lvdStat = new LocalVarStat(
                Arrays.asList(forGeneratorVar, forStateVar, forControlVar),
                node.expList
        );
        processLocalVarDeclStat(fi, lvdStat);
        for (String name : node.nameList) {
            fi.addLocVar(name, fi.pc()+2);
        }

        int pcJmpToTFC = fi.emitJmp(0, 0);
        BlockProcessor.processBlock(fi, node.block);
        fi.closeOpenUpvals( );
        fi.fixSbx(pcJmpToTFC, fi.pc()-pcJmpToTFC);

        int rGenerator = fi.slotOfLocVar(forGeneratorVar);
        fi.emitTForCall(  rGenerator, node.nameList.size());
        fi.emitTForLoop(  rGenerator+2, pcJmpToTFC-fi.pc()-1);

        fi.exitScope(fi.pc() - 1);
        fi.fixEndPC(forGeneratorVar, 2);
        fi.fixEndPC(forStateVar, 2);
        fi.fixEndPC(forControlVar, 2);
    }

    private static void processLocalVarDeclStat(FuncInformation fi, LocalVarStat node) {
        List<Exp> exps = removeTailNils(node.exps);
        int nExps = exps.size();
        int nNames = node.names.size();

        int oldRegs = fi.usedRegs;
        if (nExps == nNames) {
            for(Exp exp : exps) {
                int a = fi.allocReg();
                ExpProcessor.processExp(fi, exp, a, 1);
            }
        } else if (nExps > nNames) {
            for (int i = 0; i < exps.size(); i++) {
                Exp exp = exps.get(i);
                int a = fi.allocReg();
                if (i == nExps-1 && isVarargOrFuncCall(exp)) {
                    ExpProcessor.processExp(fi, exp, a, 0);
                } else {
                    ExpProcessor.processExp(fi, exp, a, 1);
                }
            }
        } else { // nNames > nExps
            boolean multRet = false;
            for (int i = 0; i < exps.size(); i++) {
                Exp exp = exps.get(i);
                int a = fi.allocReg();
                if (i == nExps-1 && isVarargOrFuncCall(exp)) {
                    multRet = true;
                    int n = nNames - nExps + 1;
                    ExpProcessor.processExp(fi, exp, a, n);
                    fi.allocRegs(n - 1);
                } else {
                    ExpProcessor.processExp(fi, exp, a, 1);
                }
            }
            if (!multRet) {
                int n = nNames - nExps;
                int a = fi.allocRegs(n);
                fi.emitLoadNil(  a, n);
            }
        }

        fi.usedRegs = oldRegs;
        int startPC = fi.pc() + 1;
        for (String name : node.names) {
            fi.addLocVar(name, startPC);
        }
    }

    private static void processAssignStat(FuncInformation fi, AssignStat node) {
        List<Exp> exps = removeTailNils(node.expList);
        int nExps = exps.size();
        int nVars = node.varList.size();

        int[] tRegs = new int[nVars];
        int[] kRegs = new int[nVars];
        int[] vRegs = new int[nVars];
        int oldRegs = fi.usedRegs;

        for (int i = 0; i < node.varList.size(); i++) {
            Exp exp = node.varList.get(i);
            if (exp instanceof TableVisitExp) {
                TableVisitExp taExp = (TableVisitExp) exp;
                tRegs[i] = fi.allocReg();
                ExpProcessor.processExp(fi, taExp.prefixExp, tRegs[i], 1);
                kRegs[i] = fi.allocReg();
                ExpProcessor.processExp(fi, taExp.varExp, kRegs[i], 1);
            } else {
                String name = ((LabelExp) exp).name;
                if (fi.slotOfLocVar(name) < 0 && fi.indexOfUpval(name) < 0) {
                    // global var
                    kRegs[i] = -1;
                    if (fi.indexOfConstant(name) > 0xFF) {
                        kRegs[i] = fi.allocReg();
                    }
                }
            }
        }
        for (int i = 0; i < nVars; i++) {
            vRegs[i] = fi.usedRegs + i;
        }

        if (nExps >= nVars) {
            for (int i = 0; i < exps.size(); i++) {
                Exp exp = exps.get(i);
                int a = fi.allocReg();
                if (i >= nVars && i == nExps-1 && isVarargOrFuncCall(exp)) {
                    ExpProcessor.processExp(fi, exp, a, 0);
                } else {
                    ExpProcessor.processExp(fi, exp, a, 1);
                }
            }
        } else { // nVars > nExps
            boolean multRet = false;
            for (int i = 0; i < exps.size(); i++) {
                Exp exp = exps.get(i);
                int a = fi.allocReg();
                if (i == nExps-1 && isVarargOrFuncCall(exp)) {
                    multRet = true;
                    int n = nVars - nExps + 1;
                    ExpProcessor.processExp(fi, exp, a, n);
                    fi.allocRegs(n - 1);
                } else {
                    ExpProcessor.processExp(fi, exp, a, 1);
                }
            }
            if (!multRet) {
                int n = nVars - nExps;
                int a = fi.allocRegs(n);
                fi.emitLoadNil(  a, n);
            }
        }

        for (int i = 0; i < node.varList.size(); i++) {
            Exp exp = node.varList.get(i);
            if (! (exp instanceof LabelExp)) {
                fi.emitSetTable( tRegs[i], kRegs[i], vRegs[i]);
                continue;
            }

            LabelExp nameExp = (LabelExp) exp;
            String varName = nameExp.name;
            int a = fi.slotOfLocVar(varName);
            if (a >= 0) {
                fi.emitMove( a, vRegs[i]);
                continue;
            }

            int b = fi.indexOfUpval(varName);
            if (b >= 0) {
                fi.emitSetUpval( vRegs[i], b);
                continue;
            }

            a = fi.slotOfLocVar("_ENV");
            if (a >= 0) {
                if (kRegs[i] < 0) {
                    b = 0x100 + fi.indexOfConstant(varName);
                    fi.emitSetTable( a, b, vRegs[i]);
                } else {
                    fi.emitSetTable( a, kRegs[i], vRegs[i]);
                }
                continue;
            }

            // global var
            a = fi.indexOfUpval("_ENV");
            if (kRegs[i] < 0) {
                b = 0x100 + fi.indexOfConstant(varName);
                fi.emitSetTabUp( a, b, vRegs[i]);
            } else {
                fi.emitSetTabUp( a, kRegs[i], vRegs[i]);
            }
        }

        fi.usedRegs = oldRegs;
    }
}

class BlockProcessor extends Processor{
    static void processBlock(FuncInformation fi, Block node) {
        for (Stat stat : node.stats) {
            StatProcessor.processStat(fi, stat);
        }

        if (node.exps != null) {
            processRetStat(fi, node.exps);
        }
    }

    private static void processRetStat(FuncInformation fi, List<Exp> exps) {
        int nExps = exps.size();
        if (nExps == 0) {
            fi.emitReturn( 0, 0);
            return;
        }

        if (nExps == 1) {
            if (exps.get(0) instanceof LabelExp) {
                LabelExp nameExp = (LabelExp) exps.get(0);
                int r = fi.slotOfLocVar(nameExp.name);
                if (r >= 0) {
                    fi.emitReturn( r, 1);
                    return;
                }
            }
            if (exps.get(0) instanceof FuncCallExp) {
                FuncCallExp fcExp = (FuncCallExp) exps.get(0);
                int r = fi.allocReg();
                ExpProcessor.processTailCallExp(fi, fcExp, r);
                fi.freeReg();
                fi.emitReturn( r, -1);
                return;
            }
        }

        boolean multRet = isVarargOrFuncCall(exps.get(nExps-1));
        for (int i = 0; i < nExps; i++) {
            Exp exp = exps.get(i);
            int r = fi.allocReg();
            if (i == nExps-1 && multRet) {
                ExpProcessor.processExp(fi, exp, r, -1);
            } else {
                ExpProcessor.processExp(fi, exp, r, 1);
            }
        }
        fi.freeRegs(nExps);

        int a = fi.usedRegs; // correct?
        if (multRet) {
            fi.emitReturn( a, -1);
        } else {
            fi.emitReturn( a, nExps);
        }
    }
}