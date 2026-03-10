// Generated from org/everapp/MyDSL.g4 by ANTLR 4.13.1
package org.everapp;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MyDSLParser}.
 */
public interface MyDSLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MyDSLParser#domain}.
	 * @param ctx the parse tree
	 */
	void enterDomain(MyDSLParser.DomainContext ctx);
	/**
	 * Exit a parse tree produced by {@link MyDSLParser#domain}.
	 * @param ctx the parse tree
	 */
	void exitDomain(MyDSLParser.DomainContext ctx);
	/**
	 * Enter a parse tree produced by {@link MyDSLParser#states}.
	 * @param ctx the parse tree
	 */
	void enterStates(MyDSLParser.StatesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MyDSLParser#states}.
	 * @param ctx the parse tree
	 */
	void exitStates(MyDSLParser.StatesContext ctx);
	/**
	 * Enter a parse tree produced by {@link MyDSLParser#transitions}.
	 * @param ctx the parse tree
	 */
	void enterTransitions(MyDSLParser.TransitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MyDSLParser#transitions}.
	 * @param ctx the parse tree
	 */
	void exitTransitions(MyDSLParser.TransitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MyDSLParser#transition}.
	 * @param ctx the parse tree
	 */
	void enterTransition(MyDSLParser.TransitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MyDSLParser#transition}.
	 * @param ctx the parse tree
	 */
	void exitTransition(MyDSLParser.TransitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MyDSLParser#actionName}.
	 * @param ctx the parse tree
	 */
	void enterActionName(MyDSLParser.ActionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MyDSLParser#actionName}.
	 * @param ctx the parse tree
	 */
	void exitActionName(MyDSLParser.ActionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MyDSLParser#fromState}.
	 * @param ctx the parse tree
	 */
	void enterFromState(MyDSLParser.FromStateContext ctx);
	/**
	 * Exit a parse tree produced by {@link MyDSLParser#fromState}.
	 * @param ctx the parse tree
	 */
	void exitFromState(MyDSLParser.FromStateContext ctx);
	/**
	 * Enter a parse tree produced by {@link MyDSLParser#toState}.
	 * @param ctx the parse tree
	 */
	void enterToState(MyDSLParser.ToStateContext ctx);
	/**
	 * Exit a parse tree produced by {@link MyDSLParser#toState}.
	 * @param ctx the parse tree
	 */
	void exitToState(MyDSLParser.ToStateContext ctx);
}