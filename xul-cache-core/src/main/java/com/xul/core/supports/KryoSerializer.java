package com.xul.core.supports;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * kryo序列化工具类
 *
 * @author xl
 * @date:2021-04-13
 */
public class KryoSerializer {
    /* Kryo有三组读写对象的方法
     * 1.如果不知道对象的具体类，且对象可以为null： kryo.writeClassAndObject(output, object); Object object = kryo.readClassAndObject(input);
     * 2.如果类已知且对象可以为null： kryo.writeObjectOrNull(output, someObject); SomeClass someObject = kryo.readObjectOrNull(input, SomeClass.class);
     * 3.如果类已知且对象不能为null:  kryo.writeObject(output, someObject); SomeClass someObject = kryo.readObject(input, SomeClass.class);
     */

    /**
     * （池化Kryo实例）使用ThreadLocal
     */
    private static final ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        @Override
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            /**
             * 不要轻易改变这里的配置,更改之后，序列化的格式就会发生变化，
             * 上线的同时就必须清除 Redis 里的所有缓存，
             * 否则那些缓存再回来反序列化的时候，就会报错
             */
            //支持对象循环引用（否则会栈溢出）
            kryo.setReferences(true); //默认值就是 true，添加此行的目的是为了提醒维护者，不要改变这个配置
            //不强制要求注册类（注册行为无法保证多个 JVM 内同一个类的注册编号相同；而且业务系统中大量的 Class 也难以一一注册）
            kryo.setRegistrationRequired(false); //默认值就是 false，添加此行的目的是为了提醒维护者，不要改变这个配置
            //Fix the NPE bug when deserializing Collections.
            kryo.register(UnmodifiableCollectionsSerializer.class);
            //Fix the NPE bug when deserializing Collections.
            ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy()).setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
            return kryo;
        }
    };


    /**
     * 使用ThreadLocal创建Kryo
     * 把java对象序列化成byte[];
     *
     * @param obj java对象
     * @return
     */
    public static <T> byte[] writeObjectToByteArray(T obj) {
        ByteArrayOutputStream os = null;
        Output output = null;
        if (null != obj) {
            Kryo kryo = kryos.get();
            try {
                os = new ByteArrayOutputStream();
                output = new Output(os);
                kryo.writeObject(output, obj);
                close(output);
                return os.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(os);

            }
        }
        return null;
    }

    /**
     * 使用ThreadLocal创建Kryo
     * 把List序列化成byte[];
     *
     * @param list java对象
     * @return
     */
    public static <T> byte[] writeObjectToByteArray(List<T> list) {
        ByteArrayOutputStream os = null;
        Output output = null;
        byte[] bytes = null;
        if (null != list && list.size() > 0) {
            Kryo kryo = kryos.get();
            try {
                os = new ByteArrayOutputStream();
                output = new Output(os);
                kryo.writeObject(output, list);
                close(output);
                bytes = os.toByteArray();
                return bytes;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(os);
            }
        }
        return null;
    }

    /**
     * 使用ThreadLocal创建Kryo
     * 把byte[]反序列化成指定的java对象
     *
     * @param bytes
     * @param t     指定的java对象
     * @param <T>
     * @return 指定的java对象
     */
    public static <T> T readFromByteArray(byte[] bytes, Class<T> t) {
        ByteArrayInputStream is = null;
        Input input = null;
        if (null != bytes && bytes.length > 0 && null != t) {
            try {
                Kryo kryo = kryos.get();
                is = new ByteArrayInputStream(bytes);
                input = new Input(is);
                return kryo.readObject(input, t);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(is);
                close(input);
            }
        }
        return null;
    }




    /**
     * 使用ThreadLocal创建Kryo
     * 把byte[]反序列化成指定的List<T>
     *
     * @param bytes byte数组
     * @param <T>
     * @return 指定java对象的List
     */
    public static <T> List<T> readFromByteArray(byte[] bytes) {
        ByteArrayInputStream is = null;
        Input input = null;
        if (null != bytes && bytes.length > 0) {
            try {
                Kryo kryo = kryos.get();
                is = new ByteArrayInputStream(bytes);
                input = new Input(is);
                List<T> list = kryo.readObject(input, ArrayList.class);
                return list;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(is);
                close(input);
            }
        }
        return null;
    }


    /**
     * 关闭io流对象
     *
     * @param closeable
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
