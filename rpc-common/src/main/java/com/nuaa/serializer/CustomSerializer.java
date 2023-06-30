package com.nuaa.serializer;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义序列化/反序列化方法（基于 Protostuff）
 *

 * Protostuff 以高性能而闻名，基于代码生成技术，将Java 对象转换为紧凑的二进制格式，占用更小的空间,减少了序列化和反序列化的开销。
 */
public class CustomSerializer {

    /**
     * cachedSchema:使用缓存机制来避免重复获取和生成对象结构的开销
     * Schema:1 用于描述对象的结构，包括字段的名称、类型、顺序等信息
     *        2 定义了对象的序列化和反序列化规则。它指定了字段的序列化顺序和方式
     *
     *  通过缓存和复用Schema对象，避免重复获取和生成对象结构的开销。
        此外，Schema对象对于字段的顺序和类型信息进行了优化，以提高序列化和反序列化的效率。
     * */
    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    /**用于实例化对象的库
     * 绕过构造函数实例化对象：通常情况下，使用Java反射来实例化对象需要调用构造函数。而ObjenesisStd使用了一种绕过构造函数的方法来实例化对象，从而提高了实例化的性能。
     */
    private static  Objenesis objenesis = new ObjenesisStd(true);


    /**
     * @apiNote 获取给定类的 Schema 对象。
     * 它首先尝试从缓存中获取已存在的 Schema，如果不存在则创建一个新的 Schema 并放入缓存中。
     * */
    private static <T> Schema<T> getSchema(Class<T> cls) {
        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(cls);
            cachedSchema.put(cls, schema);
        }
        return schema;
    }

    /**
     * @apiNote 方法用于将对象序列化为字节数组。 序列化 obj ——> byte[]
     * @param obj 对象
     *  首先获取该对象的类 cls，然后申请一个内存空间作为缓冲区，使用 getSchema 方法获取该类的 Schema，
     * 最后使用 ProtostuffIOUtil 的 toByteArray 方法将对象序列化为字节数组。
     */
    public static <T> byte[] serialize(T obj) {
        //1 获得序列化对象的类
        Class<T> cls = (Class<T>) obj.getClass();
        //2 为该类分配一个缓存空间 默认大小的空间512个字节
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            //3 获取该类的 Schema
            Schema<T> schema = getSchema(cls);
            //4 序列化
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     *  @apiNote 方法用于将字节数组反序列化为对象  byte[] ——> obj
     *  @param  data 字节数组  cls 类
     * 首先使用 objenesis 实例化一个空对象 message,然后使用 getSchema 方法获取该类的 Schema，
     * 最后使用 ProtostuffIOUtil 的 mergeFrom 方法将字节数组反序列化为对象 message。
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        try {
            //1 objenesis 实例化一个空对象 message
            T message = objenesis.newInstance(cls);
            //2 getSchema 方法获取该类的 Schema
            Schema<T> schema = getSchema(cls);
            //3  反序列化
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


}
