package org.lindberg.jpa.entitycloner.persistence;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * Fields with this annotation must be cloned even if the clone is not required.
 * 
 * @author Victor Lindberg (victorlindberg713@gmail.com)
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
public @interface ForceClone {
	
}
