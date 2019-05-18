package com.boluo.message.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.LocalDateTime;

/**
 * 用于设置Jersey的Json转换器
 * 用于替换JacksonJsonProvider
 * <p>
 * 该工具类完成了，把Http请求中的请求数据转换为Model实体，
 * 同时也实现了把返回的Model实体转换为Json字符串
 * 并输出到Http的返回体中。
 *
 * @param <T> 任意类型范型定义
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GsonProvider<T> implements MessageBodyReader, MessageBodyWriter {

    private static final Gson gson;

    static{
        GsonBuilder gsonBuilder = new GsonBuilder()
                .serializeNulls()
                .excludeFieldsWithoutExposeAnnotation()
                .enableComplexMapKeySerialization();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter());
        gson = gsonBuilder.create();
    }

    /**
     * 取得一个全局的Gson
     *
     * @return Gson
     */
    public static Gson getGson() {
        return gson;
    }

    public GsonProvider() {
    }

    @Override
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try ( JsonReader jsonReader = new JsonReader(new InputStreamReader(entityStream, "UTF-8"));){
            return gson.fromJson(jsonReader, genericType);
        }
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(Object o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    /**
     * 把一个实例输出到Http输出流中
     */
    @Override
    public void writeTo(Object o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try (JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(entityStream, Charset.forName("UTF-8")))){
            gson.toJson(o,genericType,jsonWriter);
        }
    }
}
