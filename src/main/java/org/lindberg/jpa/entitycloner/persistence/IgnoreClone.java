package org.lindberg.jpa.entitycloner.persistence;


import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fields with this note will not be cloned even if the clone is required.
 * 
 * @author Victor Lindberg (victorlindberg713@gmail.com)
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
public @interface IgnoreClone {
	
	/**
	 * Classes that extend of the class to which this field belongs and this field will be ignored in the clone process.
	 * 
	 */
	Class<?>[] forInheritedClasses() default Object.class;
	
	/**
	 * If true then this field will be setted to null in the clone. 
	 * If false then the original object will be setted in the clone.
	 */
	boolean setNull() default false;

}
