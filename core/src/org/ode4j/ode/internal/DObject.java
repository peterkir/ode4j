/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001,2002 Russell L. Smith.       *
 * All rights reserved.  Email: russ@q12.org   Web: www.q12.org          *
 * Open Dynamics Engine 4J, Copyright (C) 2007-2010 Tilmann Zäschke      *
 * All rights reserved.  Email: ode4j@gmx.de   Web: www.ode4j.org        *
 *                                                                       *
 * This library is free software; you can redistribute it and/or         *
 * modify it under the terms of EITHER:                                  *
 *   (1) The GNU Lesser General Public License as published by the Free  *
 *       Software Foundation; either version 2.1 of the License, or (at  *
 *       your option) any later version. The text of the GNU Lesser      *
 *       General Public License is included with this library in the     *
 *       file LICENSE.TXT.                                               *
 *   (2) The BSD-style license that is included with this library in     *
 *       the file ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT.         *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT, ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT for more   *
 * details.                                                              *
 *                                                                       *
 *************************************************************************/
package org.ode4j.ode.internal;

import org.cpp4j.java.Ref;


// base class for bodies and joints

public abstract class DObject extends DBase {
	
	public DxWorld world;		// world this object is in
//	  dObject *next;		// next object of this type in list
//	  dObject **tome;		// pointer to previous object's next ptr
//	private Ref<dObject> _next;		// next object of this type in list
	private DObject _next;		// next object of this type in list
	//private dObject _prev;		// pointer to previous object (TZ)
//	private dObject tome;		// pointer to previous object's next ptr
	private Ref<DObject> _tome;		// pointer to previous object's next ptr
	public int tag;			// used by dynamics algorithms
	//void userdata;		// user settable data
	protected Object userdata;		// user settable data
	protected DObject(DxWorld w) { //From ODE.java
		world = w;
		_next = null;
		_tome = null;
		userdata = null;
		tag = 0;
	}
	
	// add an object `obj' to the list who's head pointer is pointed 
	//to by `first'.

	//static void addObjectToList (dObject *obj, dObject **first)
	public static <T extends DObject> void addObjectToList (T obj, 
			Ref<T> first)
	{
//		System.err.println("ADDING OBJ: " + obj.getClass().getName());
		//  obj.next = *first;
		//  obj.tome = first;
		//  if (first != null) first.tome = &obj.next;
		//  first = obj;
		obj._next = first.get();
		obj._tome = (Ref<DObject>) first;
		if (first.get() != null) first.get()._tome.set(obj._next);
		first.set(obj);
	}


	// remove the object from the linked list

//	public static <T extends dObject>void removeObjectFromList (T obj)
	public <T extends DObject>void removeObjectFromList ()
	{
		//System.err.println("REMOVING OBJ: " + getClass().getName() + " / " + toString());
		//	  if (obj.next) obj.next.tome = obj.tome;
		//	  *(obj.tome) = obj.next;
		//	  // safeguard
		//	  obj.next = 0;
		//	  obj.tome = 0;
		if (_next != null) _next._tome = _tome;
		_tome.set(_next);
		// safeguard
		_next = null;
		_tome = null;
	}
	
	public DxWorld getWorld() {
		return world;
	}
	
	public DObject getNext() {
		return _next;
	}
	
	protected DObject getTome() {
		return _tome.get();
	}
}
