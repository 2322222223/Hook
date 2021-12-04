package com.rlz.processor;

import com.google.auto.service.AutoService;
import com.rlz.annotation.JudgeLogin;
import com.rlz.annotation.Login;
import com.rlz.annotation.NeedLogin;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedOptions("room.schemaLocation")
public class LoginProcessor extends AbstractProcessor {

    private static final String PN = "com.hook.ams.apt";

    private Messager mMessage;

    private List<String> mStringList;

    private String mLoginActivity;

    private String mJudgeLoginMethod;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessage = processingEnv.getMessager();
        mStringList = new ArrayList<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(NeedLogin.class.getCanonicalName());
        supportTypes.add(Login.class.getCanonicalName());
        supportTypes.add(JudgeLogin.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return false;
        }
        mMessage.printMessage(Diagnostic.Kind.WARNING, "\nprocessing...\n");

        // 1，获取所有添加了注解的Activity，保存到List中
        parseAnnotation(roundEnvironment);

        // 2，创建名为NeedLogin的类
        TypeSpec typeSpec = TypeSpec.classBuilder("LoginUtils")
                .addModifiers(Modifier.PUBLIC)

                // 3，添加获取类的list的方法
                .addMethod(createNeedLoginFun())

                // 创建登录activity相关代码
                .addMethod(createLoginActivityFun())
                .addMethod(createJudgeLoginFun())
                .build();

        // 4，设置包路径：com.rlz.ams.apt
        JavaFile javaFile = JavaFile.builder(PN, typeSpec).build();
        try {

            // 5，生成文件
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMessage.printMessage(Diagnostic.Kind.WARNING, "\nprocess finish ...\n");
        return true;// 返回false则只会执行一次
    }

    /**
     * 获取所有注解的Activity,并保存
     *
     */
    private void parseAnnotation(RoundEnvironment roundEnv) {
        mStringList.clear();

        // 得到所有注解为NeedLogin的元素
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(NeedLogin.class);
        for (Element element : elements) {
            // 检查元素是否是一个class.  注意：不能用instanceof TypeElement来判断，因为接口类型也是TypeElement.
            if (element.getKind() != ElementKind.CLASS) {
                mMessage.printMessage(Diagnostic.Kind.WARNING,
                        element.getSimpleName().toString() + "不是类，不予处理");
                continue;
            }

            // 强转成TypeElement
            TypeElement classElement = (TypeElement) element;

            // 包名+类型:com.rlz.ams.apt.activity.EventActivity
            String fullClassName = classElement.getQualifiedName().toString();

            mStringList.add(fullClassName);
        }

        // 查找登录的Activity
        Set<? extends Element> loginElements = roundEnv.getElementsAnnotatedWith(Login.class);
        for (Element loginActivityElement : loginElements) {
            if (loginActivityElement.getKind() != ElementKind.CLASS) {
                mMessage.printMessage(Diagnostic.Kind.WARNING,
                        loginActivityElement.getSimpleName().toString() + "不是类，不予处理");
                continue;
            }
            // 强转成TypeElement
            TypeElement classElement = (TypeElement) loginActivityElement;
            // 包名+类型
            mLoginActivity = classElement.getQualifiedName().toString();
        }

        // 查找判断是否登录的方法
        Set<? extends Element> judgeLoginElements = roundEnv.getElementsAnnotatedWith(JudgeLogin.class);
        for (Element element : judgeLoginElements) {
            if (element instanceof ExecutableElement) {
                mMessage.printMessage(Diagnostic.Kind.WARNING,
                        "\n判断登录的方法:" + element.getSimpleName());
                ExecutableElement method = (ExecutableElement) element;
                TypeElement classElement = (TypeElement) method.getEnclosingElement();
                mMessage.printMessage(Diagnostic.Kind.WARNING, "\n登录方法所在类：" + classElement.getQualifiedName().toString());
                String classPath = classElement.getQualifiedName().toString();
                if (classPath.endsWith("Companion")) {
                    continue;
                }
                mJudgeLoginMethod = classPath + "#" + element.getSimpleName();
            }
        }
    }

    /**
     * 创建获取注解名的方法
     */
    private MethodSpec createNeedLoginFun() {
        ClassName list = ClassName.get("java.util", "ArrayList");

        // 返回值类型 List<String>
        TypeName listOfView = ParameterizedTypeName.get(List.class, String.class);

        // 创建名为getView的方法
        MethodSpec.Builder m = MethodSpec.methodBuilder("getRequireLoginList")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(listOfView);
        m.addStatement("$T result = new $T<>()", listOfView, list);
        for (String s : mStringList) {
            m.addStatement("result.add(\"" + s + "\")");
        }
        m.addStatement("return result");
        return m.build();
    }

    /**
     * 创建登录的activity
     */
    private MethodSpec createLoginActivityFun() {
        ClassName clazzName = ClassName.get(String.class);
        MethodSpec.Builder m = MethodSpec.methodBuilder("getLoginActivity")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(clazzName);

        m.addStatement("return \"" + mLoginActivity + "\"");
        return m.build();
    }

    /**
     * 判断是否登录的方法
     */
    private MethodSpec createJudgeLoginFun() {
        ClassName clazzName = ClassName.get(String.class);
        MethodSpec.Builder m = MethodSpec.methodBuilder("getJudgeLoginMethod")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(clazzName);

        m.addStatement("return \"" + mJudgeLoginMethod + "\"");
        return m.build();
    }
}
