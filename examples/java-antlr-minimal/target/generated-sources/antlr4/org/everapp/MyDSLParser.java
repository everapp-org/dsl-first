// Generated from org/everapp/MyDSL.g4 by ANTLR 4.13.1
package org.everapp;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class MyDSLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, ID=9, 
		WS=10;
	public static final int
		RULE_domain = 0, RULE_states = 1, RULE_transitions = 2, RULE_transition = 3, 
		RULE_actionName = 4, RULE_fromState = 5, RULE_toState = 6;
	private static String[] makeRuleNames() {
		return new String[] {
			"domain", "states", "transitions", "transition", "actionName", "fromState", 
			"toState"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'domain'", "'{'", "'}'", "'states'", "','", "'transitions'", "':'", 
			"'->'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, "ID", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "MyDSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public MyDSLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DomainContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(MyDSLParser.ID, 0); }
		public StatesContext states() {
			return getRuleContext(StatesContext.class,0);
		}
		public TransitionsContext transitions() {
			return getRuleContext(TransitionsContext.class,0);
		}
		public DomainContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_domain; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).enterDomain(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).exitDomain(this);
		}
	}

	public final DomainContext domain() throws RecognitionException {
		DomainContext _localctx = new DomainContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_domain);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(14);
			match(T__0);
			setState(15);
			match(ID);
			setState(16);
			match(T__1);
			setState(17);
			states();
			setState(18);
			transitions();
			setState(19);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatesContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(MyDSLParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(MyDSLParser.ID, i);
		}
		public StatesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_states; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).enterStates(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).exitStates(this);
		}
	}

	public final StatesContext states() throws RecognitionException {
		StatesContext _localctx = new StatesContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_states);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(21);
			match(T__3);
			setState(22);
			match(T__1);
			setState(23);
			match(ID);
			setState(28);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__4) {
				{
				{
				setState(24);
				match(T__4);
				setState(25);
				match(ID);
				}
				}
				setState(30);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(31);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TransitionsContext extends ParserRuleContext {
		public List<TransitionContext> transition() {
			return getRuleContexts(TransitionContext.class);
		}
		public TransitionContext transition(int i) {
			return getRuleContext(TransitionContext.class,i);
		}
		public TransitionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transitions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).enterTransitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).exitTransitions(this);
		}
	}

	public final TransitionsContext transitions() throws RecognitionException {
		TransitionsContext _localctx = new TransitionsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_transitions);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(33);
			match(T__5);
			setState(34);
			match(T__1);
			setState(38);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(35);
				transition();
				}
				}
				setState(40);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(41);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TransitionContext extends ParserRuleContext {
		public ActionNameContext actionName() {
			return getRuleContext(ActionNameContext.class,0);
		}
		public FromStateContext fromState() {
			return getRuleContext(FromStateContext.class,0);
		}
		public ToStateContext toState() {
			return getRuleContext(ToStateContext.class,0);
		}
		public TransitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).enterTransition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).exitTransition(this);
		}
	}

	public final TransitionContext transition() throws RecognitionException {
		TransitionContext _localctx = new TransitionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_transition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(43);
			actionName();
			setState(44);
			match(T__6);
			setState(45);
			fromState();
			setState(46);
			match(T__7);
			setState(47);
			toState();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ActionNameContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(MyDSLParser.ID, 0); }
		public ActionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).enterActionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).exitActionName(this);
		}
	}

	public final ActionNameContext actionName() throws RecognitionException {
		ActionNameContext _localctx = new ActionNameContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_actionName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(49);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FromStateContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(MyDSLParser.ID, 0); }
		public FromStateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromState; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).enterFromState(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).exitFromState(this);
		}
	}

	public final FromStateContext fromState() throws RecognitionException {
		FromStateContext _localctx = new FromStateContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_fromState);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(51);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ToStateContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(MyDSLParser.ID, 0); }
		public ToStateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_toState; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).enterToState(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MyDSLListener ) ((MyDSLListener)listener).exitToState(this);
		}
	}

	public final ToStateContext toState() throws RecognitionException {
		ToStateContext _localctx = new ToStateContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_toState);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(53);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001\n8\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0005\u0001\u001b\b\u0001\n"+
		"\u0001\f\u0001\u001e\t\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0005\u0002%\b\u0002\n\u0002\f\u0002(\t\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0000\u0000\u0007\u0000\u0002\u0004\u0006"+
		"\b\n\f\u0000\u00002\u0000\u000e\u0001\u0000\u0000\u0000\u0002\u0015\u0001"+
		"\u0000\u0000\u0000\u0004!\u0001\u0000\u0000\u0000\u0006+\u0001\u0000\u0000"+
		"\u0000\b1\u0001\u0000\u0000\u0000\n3\u0001\u0000\u0000\u0000\f5\u0001"+
		"\u0000\u0000\u0000\u000e\u000f\u0005\u0001\u0000\u0000\u000f\u0010\u0005"+
		"\t\u0000\u0000\u0010\u0011\u0005\u0002\u0000\u0000\u0011\u0012\u0003\u0002"+
		"\u0001\u0000\u0012\u0013\u0003\u0004\u0002\u0000\u0013\u0014\u0005\u0003"+
		"\u0000\u0000\u0014\u0001\u0001\u0000\u0000\u0000\u0015\u0016\u0005\u0004"+
		"\u0000\u0000\u0016\u0017\u0005\u0002\u0000\u0000\u0017\u001c\u0005\t\u0000"+
		"\u0000\u0018\u0019\u0005\u0005\u0000\u0000\u0019\u001b\u0005\t\u0000\u0000"+
		"\u001a\u0018\u0001\u0000\u0000\u0000\u001b\u001e\u0001\u0000\u0000\u0000"+
		"\u001c\u001a\u0001\u0000\u0000\u0000\u001c\u001d\u0001\u0000\u0000\u0000"+
		"\u001d\u001f\u0001\u0000\u0000\u0000\u001e\u001c\u0001\u0000\u0000\u0000"+
		"\u001f \u0005\u0003\u0000\u0000 \u0003\u0001\u0000\u0000\u0000!\"\u0005"+
		"\u0006\u0000\u0000\"&\u0005\u0002\u0000\u0000#%\u0003\u0006\u0003\u0000"+
		"$#\u0001\u0000\u0000\u0000%(\u0001\u0000\u0000\u0000&$\u0001\u0000\u0000"+
		"\u0000&\'\u0001\u0000\u0000\u0000\')\u0001\u0000\u0000\u0000(&\u0001\u0000"+
		"\u0000\u0000)*\u0005\u0003\u0000\u0000*\u0005\u0001\u0000\u0000\u0000"+
		"+,\u0003\b\u0004\u0000,-\u0005\u0007\u0000\u0000-.\u0003\n\u0005\u0000"+
		"./\u0005\b\u0000\u0000/0\u0003\f\u0006\u00000\u0007\u0001\u0000\u0000"+
		"\u000012\u0005\t\u0000\u00002\t\u0001\u0000\u0000\u000034\u0005\t\u0000"+
		"\u00004\u000b\u0001\u0000\u0000\u000056\u0005\t\u0000\u00006\r\u0001\u0000"+
		"\u0000\u0000\u0002\u001c&";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}