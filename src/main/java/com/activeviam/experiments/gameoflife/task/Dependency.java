package com.activeviam.experiments.gameoflife.task;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used for marking subtasks a task depends on. Dependencies are automatically disposed using
 * {@link ATask#dispose()} method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Dependency {

}
