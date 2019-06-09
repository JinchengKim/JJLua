# CMPS203 Project Java-based Lua Interpreter


## Introduction

Lua  is  a  powerful,  efficient,  lightweight,  and  embeddablescripting  language.  In  this  project,  we  extended  theknowledge  from  lectures  of  Programming  Languages  andimplemented  an  interpreter  of  Lua.  This  report  describesthe techniques, data structures and algorithms for interpretingLua language using Java and JVM. To illustrate the issues andtechniques  in  interpreting  Lua,  we  implemented  the  Lexer,Parser,  AST,  Interpreter  and  Execution  phases  for  our  Java-based  Lua  Interpreter.  

## Architecture

In general, the design of Lua interpreter can be divided into front-end and back-end stages. The front-end stage contains source code  analysis,  lexer,  parser  phases,  and  the  back-end  stagecontains interpreting and executing. To  be  more  specific,  we  have  five  main  phases  which  areprepercessing  with  Lua  source  code,  lexer,  parser,  interpreterand execution result. In the interpreter, we have two differentmodes  to  interpret  our  abstract  syntax  tree.  In  the  lexer  andparser, we both include the exception catching system to helpour interpreter to handle specific lexer or parser issues. Eachtime we can take a Lua source code as input then execute theLua code and gain the execution result.

