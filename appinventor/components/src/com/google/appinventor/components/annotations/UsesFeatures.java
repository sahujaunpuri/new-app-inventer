package com.google.appinventor.components.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UsesFeatures {

	  /**
	   * The names of the permissions separated by commas.
	   *
	   * @return  the permission name
	   * @see android.Manifest.permission
	   */
	  String featureNames() default "";
}
