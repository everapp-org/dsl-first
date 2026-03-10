grammar MyDSL;

// Parser rules
domain: 'domain' ID '{' states transitions '}';
states: 'states' '{' ID (',' ID)* '}';
transitions: 'transitions' '{' transition* '}';
transition: actionName ':' fromState '->' toState;

actionName: ID;
fromState: ID;
toState: ID;

// Lexer rules
ID: [a-zA-Z_][a-zA-Z0-9_]*;
WS: [ \t\r\n]+ -> skip;
