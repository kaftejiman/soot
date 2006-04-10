/* Soot - a J*va Optimization Framework
 * Copyright (C) 2005 Nomair A. Naeem
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package soot.dava.toolkits.base.AST.transformations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.dava.internal.javaRep.DNewInvokeExpr;
import soot.dava.internal.javaRep.DVirtualInvokeExpr;
import soot.dava.toolkits.base.AST.analysis.DepthFirstAdapter;
import soot.grimp.internal.GAddExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
//import soot.jimple.internal.InvokeExprBox;



/*
 * Matches the output pattern
 *   System.out.println( (new StringBuffer()).append ............ .toString());
 *   Convert it to 
 *   System.out.println(append1 + append2 .....);
 */

public class SystemOutPrintlnCleaner extends DepthFirstAdapter {
	
	public static boolean DEBUG=false;
	
    public SystemOutPrintlnCleaner(){
    	
    }
    
    public SystemOutPrintlnCleaner(boolean verbose){
    	super(verbose);
    }

    public void inInvokeStmt(InvokeStmt s){
    	if(DEBUG)
    		System.out.println("\n\nIn an invoke stmt");
    	
    	ValueBox invokeBox = s.getInvokeExprBox();
    	if(DEBUG)
    		System.out.println("InvokeExpr is"+invokeBox.getValue());
    	
    	Value val = invokeBox.getValue();
    	if(! (val instanceof InvokeExpr))
    		return;
    	
    	InvokeExpr invokeExpr = (InvokeExpr)val;
    	
    	SootMethod methodInvoked = invokeExpr.getMethod();
    	
    	SootClass methodInvokedClass = methodInvoked.getDeclaringClass();
    	
    	if(DEBUG)
    		System.out.println("Method was invoked for class "+methodInvokedClass.toString());
    	
    	
    	if(!methodInvokedClass.toString().equals("java.io.PrintStream"))
    		return;

    	
    	if(DEBUG)
    		System.out.println("Invoked Method is"+invokeExpr.getMethod().toString());
 

    	if(! methodInvoked.toString().equals("<java.io.PrintStream: void println(java.lang.String)>"))
    		return;
    	
    	//    	its a call of System.out.println( STRING )
    	//check whether we need to simplify its argument
    	if(DEBUG)
    		System.out.println("Found System.out.println stmt going to check if this needs simplification");
    	
    	//we know there is only one argument get that out
    	ValueBox argBox = invokeExpr.getArgBox(0);
    	if(DEBUG)
    		System.out.println("Argument to System.out.println is: "+argBox.toString());
    	
    	Value tempArgValue = argBox.getValue();
    	if(DEBUG)
    		System.out.println("arg value is: "+tempArgValue);
    	
    	
    	if(! (tempArgValue instanceof DVirtualInvokeExpr)){
    		if(DEBUG)
    			System.out.println("Not a DVirtualInvokeExpr"+tempArgValue.getClass());
    		return;
    	}
    		
    	//check this is a toString for StringBuffer
    	if(DEBUG)
    		System.out.println("arg value is a virtual invokeExpr");
    	DVirtualInvokeExpr vInvokeExpr = ((DVirtualInvokeExpr)tempArgValue);
    	
    	if( ! (vInvokeExpr.getMethod().toString().equals("<java.lang.StringBuffer: java.lang.String toString()>")))
    		return;
    	
    	if(DEBUG)
    		System.out.println("Ends in toString()");
    	
    	Value base = vInvokeExpr.getBase();
    	List args = new ArrayList();
    	while( base instanceof DVirtualInvokeExpr){
    		DVirtualInvokeExpr tempV = (DVirtualInvokeExpr)base;
    		if(DEBUG)
    			System.out.println("base method is "+tempV.getMethod());
    		if(!tempV.getMethod().toString().startsWith("<java.lang.StringBuffer: java.lang.StringBuffer append")){
    			if(DEBUG)
    				System.out.println("Found a virtual invoke which is not a append"+tempV.getMethod());
    			return;
    		}
    		args.add(0,tempV.getArg(0));
    		//System.out.println("Append: "+((DVirtualInvokeExpr)base).getArg(0) );
    		//move to next base
    		base = ((DVirtualInvokeExpr)base).getBase();
    	}
    	
    	if(! (base instanceof DNewInvokeExpr ))
    		return;
    	
    	if(DEBUG)
    		System.out.println("New expr is "+ ((DNewInvokeExpr)base).getMethod() );
    	
    	if(!  ((DNewInvokeExpr)base).getMethod().toString().equals("<java.lang.StringBuffer: void <init>()>") )
    			return;
    	
    	/*
    	 * The arg is a new invoke expr of StringBuffer and all the appends are present in the args list
    	 */
    	if(DEBUG)
    		System.out.println("Found a System.out.println with a new StringBuffer.append list in it");
    	
    	//argBox contains the argument to System.out.println()
    	Iterator it = args.iterator();
    	Value newVal = null;
    	while(it.hasNext()){
    		Value temp = (Value)it.next();
    		if(newVal == null)
    			newVal = temp;
    		else{
    			//create newVal + temp
    			newVal = new GAddExpr(newVal,temp);
    		}
    		
    	}
    	if(DEBUG)
    		System.out.println("New expression for System.out.println is"+newVal);
    	
    	argBox.setValue(newVal);
    }
}
