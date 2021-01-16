/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.dependencies.classloader;

import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.function.Function;

public class ReflectionClassLoader implements PluginClassLoader {

    private static URLClassLoader extractClassLoaderFromBootstrap(LuckPermsBootstrap bootstrap) {
        return castToUrlClassLoader(bootstrap.getClass().getClassLoader());
    }

    public static URLClassLoader castToUrlClassLoader(Object classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return (URLClassLoader) classLoader;
        } else {
            throw new IllegalStateException("ClassLoader is not instance of URLClassLoader: " + classLoader.getClass().getName());
        }
    }

    private static final Method ADD_URL_METHOD;

    static {
        // If on Java 9+, open the URLClassLoader module to this module
        // so we can access its API via reflection without producing a warning.
        try {
            openUrlClassLoaderModule();
        } catch (Throwable e) {
            // ignore exception - will throw on Java 8 since the Module classes don't exist
        }

        // Get the protected 'addURL' method on URLClassLoader and set it to accessible.
        try {
            ADD_URL_METHOD = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            ADD_URL_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final URLClassLoader classLoader;

    public ReflectionClassLoader(LuckPermsBootstrap bootstrap) throws IllegalStateException {
        this(bootstrap, ReflectionClassLoader::extractClassLoaderFromBootstrap);
    }

    public ReflectionClassLoader(LuckPermsBootstrap bootstrap, Function<LuckPermsBootstrap, ? extends URLClassLoader> classLoader) {
        this.classLoader = classLoader.apply(bootstrap);
    }

    @Override
    public void addJarToClasspath(Path file) {
        try {
            ADD_URL_METHOD.invoke(this.classLoader, file.toUri().toURL());
        } catch (IllegalAccessException | InvocationTargetException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static void openUrlClassLoaderModule() throws Exception {
        // This is effectively calling:
        //
        // URLClassLoader.class.getModule().addOpens(
        //     URLClassLoader.class.getPackageName(),
        //     ReflectionClassLoader.class.getModule()
        // );
        //
        // We use reflection since we build against Java 8.

        Class<?> moduleClass = Class.forName("java.lang.Module");
        Method getModuleMethod = Class.class.getMethod("getModule");
        Method addOpensMethod = moduleClass.getMethod("addOpens", String.class, moduleClass);

        Object urlClassLoaderModule = getModuleMethod.invoke(URLClassLoader.class);
        Object thisModule = getModuleMethod.invoke(ReflectionClassLoader.class);

        addOpensMethod.invoke(urlClassLoaderModule, URLClassLoader.class.getPackage().getName(), thisModule);
    }
}
