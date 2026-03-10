// Generated from org/everapp/MyDSL.g4 by ANTLR 4.13.1
package org.everapp;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class MyDSLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, ID=9, 
		WS=10;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "ID", 
			"WS"
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


	public MyDSLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "MyDSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\nH\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b"+
		"\u0005\b=\b\b\n\b\f\b@\t\b\u0001\t\u0004\tC\b\t\u000b\t\f\tD\u0001\t\u0001"+
		"\t\u0000\u0000\n\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005"+
		"\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0001\u0000\u0003\u0003\u0000"+
		"AZ__az\u0004\u000009AZ__az\u0003\u0000\t\n\r\r  I\u0000\u0001\u0001\u0000"+
		"\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000"+
		"\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000"+
		"\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000"+
		"\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000"+
		"\u0000\u0013\u0001\u0000\u0000\u0000\u0001\u0015\u0001\u0000\u0000\u0000"+
		"\u0003\u001c\u0001\u0000\u0000\u0000\u0005\u001e\u0001\u0000\u0000\u0000"+
		"\u0007 \u0001\u0000\u0000\u0000\t\'\u0001\u0000\u0000\u0000\u000b)\u0001"+
		"\u0000\u0000\u0000\r5\u0001\u0000\u0000\u0000\u000f7\u0001\u0000\u0000"+
		"\u0000\u0011:\u0001\u0000\u0000\u0000\u0013B\u0001\u0000\u0000\u0000\u0015"+
		"\u0016\u0005d\u0000\u0000\u0016\u0017\u0005o\u0000\u0000\u0017\u0018\u0005"+
		"m\u0000\u0000\u0018\u0019\u0005a\u0000\u0000\u0019\u001a\u0005i\u0000"+
		"\u0000\u001a\u001b\u0005n\u0000\u0000\u001b\u0002\u0001\u0000\u0000\u0000"+
		"\u001c\u001d\u0005{\u0000\u0000\u001d\u0004\u0001\u0000\u0000\u0000\u001e"+
		"\u001f\u0005}\u0000\u0000\u001f\u0006\u0001\u0000\u0000\u0000 !\u0005"+
		"s\u0000\u0000!\"\u0005t\u0000\u0000\"#\u0005a\u0000\u0000#$\u0005t\u0000"+
		"\u0000$%\u0005e\u0000\u0000%&\u0005s\u0000\u0000&\b\u0001\u0000\u0000"+
		"\u0000\'(\u0005,\u0000\u0000(\n\u0001\u0000\u0000\u0000)*\u0005t\u0000"+
		"\u0000*+\u0005r\u0000\u0000+,\u0005a\u0000\u0000,-\u0005n\u0000\u0000"+
		"-.\u0005s\u0000\u0000./\u0005i\u0000\u0000/0\u0005t\u0000\u000001\u0005"+
		"i\u0000\u000012\u0005o\u0000\u000023\u0005n\u0000\u000034\u0005s\u0000"+
		"\u00004\f\u0001\u0000\u0000\u000056\u0005:\u0000\u00006\u000e\u0001\u0000"+
		"\u0000\u000078\u0005-\u0000\u000089\u0005>\u0000\u00009\u0010\u0001\u0000"+
		"\u0000\u0000:>\u0007\u0000\u0000\u0000;=\u0007\u0001\u0000\u0000<;\u0001"+
		"\u0000\u0000\u0000=@\u0001\u0000\u0000\u0000><\u0001\u0000\u0000\u0000"+
		">?\u0001\u0000\u0000\u0000?\u0012\u0001\u0000\u0000\u0000@>\u0001\u0000"+
		"\u0000\u0000AC\u0007\u0002\u0000\u0000BA\u0001\u0000\u0000\u0000CD\u0001"+
		"\u0000\u0000\u0000DB\u0001\u0000\u0000\u0000DE\u0001\u0000\u0000\u0000"+
		"EF\u0001\u0000\u0000\u0000FG\u0006\t\u0000\u0000G\u0014\u0001\u0000\u0000"+
		"\u0000\u0003\u0000>D\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}