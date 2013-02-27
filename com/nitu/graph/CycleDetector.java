/*
 * Copyright 2013 Nitu chiring
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nitu.graph;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Stack;

/**
 * Detects cyclic references in a class definition graph.
 * 
 * <pre>
 * Example,
 * 	Class A -> Class B -> Class A
 * </pre>
 * The class is not thread safe.
 * 
 * @author nchiring
 *
 */
public class CycleDetector {
	/**
	 * tracks the class objects encountered during a depth first search(dfs) traversal
	 */
	private final Stack<Class<?>> visited = new Stack<Class<?>>();
      
    private boolean cycle = false;
    
    private Class<?> cls=null;
    private boolean includeStaticField = false;
    
    /**
     * 
     * @param cls class to search with
     * @param includeStaticField boolean flag to specify whether to exempt static fields of a class
     * 
     * @throws IllegalArgumentException if {@code cls==null }
     */
    public CycleDetector(Class<?> cls, boolean includeStaticField) {
		if(cls==null){
			throw new IllegalArgumentException("Argument cls is null");
		}
		this.cls = cls;
		this.includeStaticField = includeStaticField;
	}

	/**
     * Detects whether the specified class {@code cls} leads to a cyclic reference dependency. The implementation
     * traverses the entire class definition graph starting from the specified {@code cls} class.
     *    
     * @return true if a cycle(at least) is found; false other wise.
     */
	public boolean hasCycle(){
		if( isExcludable(cls) ){
			return cycle=false;
		}
		visited.clear();
		dfs(cls);
		return cycle;
		
	}
	
	/**
	 * DFS search for cycle. Traversal is terminated after the first cycle is detected.
	 * 
	 * @param clazz
	 * @param staticField
	 */
	private void dfs(Class<?> clazz){
		   if(cycle){
			   return;
		   }
		   if( visited.contains(clazz)){
			   visited.push(clazz);
			   cycle = true;
			   return;
		   }
		   visited.push(clazz);
		   
	       Field[] fields = clazz.getDeclaredFields();
		   		   
		   for( Field f: fields ){
			   
			    if( !this.includeStaticField && Modifier.isStatic(f.getModifiers() ) ){
					  continue;
				}
			   
			    Type type = f.getGenericType();
			    Class<?> childCls = null;
			    if (type instanceof ParameterizedType) {
			        ParameterizedType pType = (ParameterizedType)type;
			        childCls= (Class<?>) pType.getActualTypeArguments()[0];
			        Class<?> rawCls = (Class<?>) pType.getRawType();
			        if( !isExcludable(rawCls) ){
						   dfs(rawCls);
					}
			    } else {
			    	childCls = f.getType();
			    }
			    if( !isExcludable(childCls) ){
			    	dfs(childCls);
			    }
			    
		   }

		   if( !cycle){ //in case a cycle has been found, we want to preserve the stack 
			            //contents so that we can report back the cycle participants 
			  visited.pop();
		  }
		   
	}
	
	/**
	 * By default, we don't traverse primitive, interface or java.lang.* types for cycle
	 * this method determines whether a given class is to be considered during dfs search or not
	 * 
	 * @param clazz
	 * @return
	 */
	private boolean isExcludable( Class<?> clazz ){
		if( clazz.isPrimitive() || clazz.isInterface() ){
			return true;
		}
		Package pack = clazz.getPackage();
		return pack==null || pack.getName().startsWith("java.lang");
	}
	
	/**
	 * spits out a string representation of the classes involved in a cycle.
	 * This method should be invoked after invoking {@link #hasCycle(Class, boolean) ) first.
	 * Note, more than one cycles may exist, but the implementation returns only one cycle.
	 * 
	 * @return the classes involved in a cycle if a cycle has been detected by {@link #hasCycle(Class, boolean) ); otherwise returns null.
	 * 
	 */
	
	public String getCycle( ){
		if( !cycle ){
			return null;
		}
		StringBuilder items = new StringBuilder();
		Class<?> terminal = visited.pop();
		read(terminal, items );
		items.append(terminal.getName() );
		
		return items.toString();
	}
    /**
     * retrieves the class objects with cyclic references from the {@code visited} stack.
     * 
     * @param terminal class where the current cycle begins at.
     * @param items
     */
	private void read(Class<?> terminal, StringBuilder items ){
		if(visited.isEmpty() ) return;
		Class<?> next =  visited.pop() ;
		if( next != terminal ){
			read( terminal, items);
		}
		items.append(next.getName() ).append("->");
	}
	
  
	/*
	public static void main(String[] args){
		CycleDetector cycle = new CycleDetector(ClassA.class, false);
		boolean hasCycle = cycle.hasCycle() ;
		if(hasCycle)
		     System.out.println("Has a cycle with " + cycle.getCycle() );
		else{
			System.out.println("Has no cycle");
		}
	}
	*/
	
}
