package eu.stamp_project.reneri;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.lang.annotation.Annotation;
import java.util.*;

public class TestClassFinder {

    public final static String TEST_METHOD_ANNOTATION = "org.junit.Test";

    public final static String RUN_WITH_CLASS_ANNOTATION = "org.junit.RunWith";

    public final static String TEST_CASE_BASE_CLASS = "junit.framework.TestCase";

    public static boolean isTestClass(CtClass<?> type) {
        return isJunit3TestClass(type) || isJunit4TestClass(type);
    }

    public static boolean isJunit3TestClass(CtClass<?> type) {
        CtTypeReference<?> superClass = type.getSuperclass();
        while (superClass != null) {
            if (referencesJunit3TestCase(superClass)) {
                return true;
            }
            superClass = superClass.getSuperclass();
        }
        return false;
    }

    public static boolean isJunit4TestClass(CtClass<?> type) {
        if(isDirectJUnit4TestClass(type)) {
            return true;
        }
        CtTypeReference<?> superClass = type.getSuperclass();
        while (superClass != null) {

            CtType<?> declaration = superClass.getDeclaration();
            if(declaration instanceof CtClass && isDirectJUnit4TestClass((CtClass)declaration)) {
                return true;
            }

            superClass = superClass.getSuperclass();
        }
        return false;
    }

    public static Set<CtClass<?>> findTestClasses(CtModel model) {
        return new HashSet<>(model.getElements(TestClassFinder::isTestClass));
    }

    public static Set<CtClass<?>> findTestClousure(Set<CtClass<?>> initialTypes) {

        Set<CtClass<?>> clousure = new HashSet<>();

        for(CtType<?> type : initialTypes) {
            List<CtTypeReference<?>> branch = getInheritanceBranch(type);
            for(int index = 0; index < branch.size(); index++) {

                CtTypeReference<?> reference = branch.get(index);
                if(referencesJunit3TestCase(reference)) {
                    // Descendants are Junit3 test cases
                    addClassReferencesFrom(branch, index + 1, clousure);
                    break;
                }
                if(!reference.isClass()) { // This method returns false also in the case where there is no definition available i.e. not following the classpath to build the model
                    continue;
                }
                CtClass<?> declaration = (CtClass<?>)reference.getDeclaration();
                if(clousure.contains(declaration)) {
                    // If it is contained in the clousure, then all descendants should be added
                     addClassReferencesFrom(branch, index + 1, clousure);
                     break;
                }

                if(isDirectJUnit4TestClass(declaration)) {
                    // Add them all from the top of the hierarchy
                    addClassReferencesFrom(branch, 0, clousure);
                }
            }
        }

        return clousure;
    }

    protected static List<CtTypeReference<?>> getInheritanceBranch(CtType<?> type) {
        ArrayList<CtTypeReference<?>> branch = new ArrayList<>();
        branch.add(type.getReference());

        CtTypeReference<?> superType = type.getSuperclass();

        while(superType != null) {
            branch.add(0, superType);

            superType = superType.getSuperclass();
        }

        return branch;
    }

    private static void addClassReferencesFrom(List<CtTypeReference<?>> list, int index, Set<CtClass<?>> to) {
        for(int i = index; i < list.size(); i++) {
            CtType<?> declaration = list.get(index).getDeclaration();
            if(declaration instanceof CtClass) {
                to.add((CtClass)declaration);
            }
        }
    }

    public static boolean referencesJunit3TestCase(CtTypeReference<?> reference) {
        return reference.getQualifiedName().equals(TEST_CASE_BASE_CLASS);
    }

    public static boolean isDirectJUnit4TestClass(CtClass<?> type) {
        return declaresJUnit4TestMethods(type) || hasAnnotation(type, RUN_WITH_CLASS_ANNOTATION);
    }

    public static boolean declaresJUnit4TestMethods(CtClass<?> type) {
        return type.getMethods().stream().anyMatch(TestClassFinder::isJunit4TestMethod);
    }

    public static boolean isJunit4TestMethod(CtMethod<?> method) {
        return hasAnnotation(method, TEST_METHOD_ANNOTATION);
    }

    public static boolean hasAnnotation(CtElement element, String qualifiedName) {
        return getAnnotation(element, qualifiedName).isPresent();
    }

    public static Optional<CtAnnotation<? extends Annotation>> getAnnotation(CtElement element, String qualifiedName) {
        return element.getAnnotations().stream().filter(annotation -> isAnnotation(annotation, qualifiedName)).findAny();
    }

    public static boolean isAnnotation(CtAnnotation<? extends Annotation> annotation, String qualifiedName) {
        return annotation.getAnnotationType().getQualifiedName().equals(qualifiedName);
    }

}
