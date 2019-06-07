package Operation;

import State.*;
/**
 * Created by lijin on 5/23/19.
 */
public enum  OprEnum {
    /*       T  A    B       C     mode */
    MOVE    (InstrImpl::move),
    LOADK   (InstrImpl::loadK),
    LOADKX  (InstrImpl::loadKx),
    LOADBOOL(InstrImpl::loadBool),
    LOADNIL (InstrImpl::loadNil),
    GETUPVAL(InstrImpl::getUpval),
    GETTABUP(InstrImpl::getTabUp),
    GETTABLE(InstrImpl::getTable),
    SETTABUP(InstrImpl::setTabUp),
    SETUPVAL(InstrImpl::setUpval),
    SETTABLE(InstrImpl::setTable),
    NEWTABLE(InstrImpl::newTable),
    SELF    (InstrImpl::self),
    ADD     (InstrImpl::add),
    SUB     (InstrImpl::sub),
    MUL     (InstrImpl::mul),
    MOD     (InstrImpl::mod),
    POW     (InstrImpl::pow),
    DIV     (InstrImpl::div),
    IDIV    (InstrImpl::idiv),
    BAND    (InstrImpl::band),
    BOR     (InstrImpl::bor),
    BXOR    (InstrImpl::bxor),
    SHL     (InstrImpl::shl),
    SHR     (InstrImpl::shr),
    UNM     (InstrImpl::unm),
    BNOT    (InstrImpl::bnot),
    NOT     (InstrImpl::not),
    LEN     (InstrImpl::length),
    CONCAT  (InstrImpl::concat),
    JMP     (InstrImpl::jmp),
    EQ      (InstrImpl::eq),
    LT      (InstrImpl::lt),
    LE      (InstrImpl::le),
    CALL    (InstrImpl::call),
    TAILCALL(InstrImpl::tailCall),
    RETURN  (InstrImpl::_return),
    FORLOOP (InstrImpl::forLoop),
    FORPREP (InstrImpl::forPrep),
    TFORCALL(InstrImpl::tForCall),
    TFORLOOP(InstrImpl::tForLoop),
    SETLIST (InstrImpl::setList),
    CLOSURE (InstrImpl::closure),
    VARARG  (InstrImpl::vararg),
    TEST    (InstrImpl::test),
    TESTSET (InstrImpl::testSet),
    EXTRAARG(null),
    ;

    InstrAction action;
    OprEnum(InstrAction action) {
        this.action = action;
    }

    public InstrAction getAction() {

        return action;
    }
}
