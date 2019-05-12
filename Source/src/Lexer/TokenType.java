package Lexer;

/**
 * Created by lijin on 5/9/19.
 */
public enum TokenType {
    EOF, // end of file

    // Keywords
    // and       break     do        else      elseif
    // end       false     for       function  if
    // in        local     nil       not       or
    // repeat    return    then      true      until     while
    KW_AND, // Operation
    KW_BREAK,
    KW_DO,
    KW_ELSE,
    KW_ELSEIF,
    KW_END,
    KW_FALSE,
    KW_FOR,
    KW_FUNCTION,
    KW_IF,
    KW_IN,
    KW_LOCAL,
    KW_NIL,
    KW_NOT, // Operation
    KW_OR, // Operation
    KW_REPEAT,
    KW_RETURN,
    KW_THEN,
    KW_TRUE,
    KW_UNTIL,
    KW_WHILE,

    // Operation
//    +     -     *     /     %     ^     #
//                 ==    ~=    <=    >=    <     >     =
//            (     )     {     }     [     ]
//    ;     :     ,     .     ..    ...

    OP_ASSIGN  , // =
    OP_MINUS   , // - (sub or unm)
    OP_WAVE    , // ~ (bnot or bxor)
    OP_ADD     , // +
    OP_MUL     , // *
    OP_DIV     , // /
    OP_IDIV    , // //
    OP_POW     , // ^
    OP_MOD     , // %
    OP_BAND    , // &
    OP_BOR     , // |
    OP_SHR     , // >>
    OP_SHL     , // <<
    OP_CONCAT  , // ..
    OP_LT      , // <
    OP_LE      , // <=
    OP_GT      , // >
    OP_GE      , // >=
    OP_EQ      , // ==
    OP_NE      , // ~=
    OP_LEN     , // #
    OP_AND     , // and
    OP_OR      , // or
    OP_NOT     , // not

    //SEP

    SEP_SEMI   , // ;
    SEP_COMMA  , // ,
    SEP_DOT    , // .
    SEP_COLON  , // :
    SEP_LABEL  , // ::
    SEP_LPAREN , // (
    SEP_RPAREN , // )
    SEP_LBRACK , // [
    SEP_RBRACK , // ]
    SEP_LCURLY , // {
    SEP_RCURLY , // }


    // DataStructure
    STRING,
    NUMBER,
    VARARG     , // ...

    // 别人的 参考一下
//    TOKEN_EOF        , // end-of-file
//    TOKEN_VARARG     , // ...



//    TOKEN_KW_BREAK   , // break
//    TOKEN_KW_DO      , // do
//    TOKEN_KW_ELSE    , // else
//    TOKEN_KW_ELSEIF  , // elseif
//    TOKEN_KW_END     , // end
//    TOKEN_KW_FALSE   , // false
//    TOKEN_KW_FOR     , // for
//    TOKEN_KW_FUNCTION, // function
//    TOKEN_KW_GOTO    , // goto
//    TOKEN_KW_IF      , // if
//    TOKEN_KW_IN      , // in
//    TOKEN_KW_LOCAL   , // local
//    TOKEN_KW_NIL     , // nil
//    TOKEN_KW_REPEAT  , // repeat
//    TOKEN_KW_RETURN  , // return
//    TOKEN_KW_THEN    , // then
//    TOKEN_KW_TRUE    , // true
//    TOKEN_KW_UNTIL   , // until
//    TOKEN_KW_WHILE   , // while
//    TOKEN_IDENTIFIER , // identifier
//    TOKEN_NUMBER     , // number literal
//    TOKEN_STRING     , // string literal
//    TOKEN_OP_UNM     , // = TOKEN_OP_MINUS // unary minus
//    TOKEN_OP_SUB     , // = TOKEN_OP_MINUS
//    TOKEN_OP_BNOT    , // = TOKEN_OP_WAVE
//    TOKEN_OP_BXOR    , // = TOKEN_OP_WAVE
}
