package org.hangu.center.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 类与字节码符号转化工具
 *
 * @author wuzhenhong
 * @date 2023/8/1 9:06
 */
public final class DescClassUtils {

    private static final Map<Class<?>, Character> PRIMITIVE_MAP_DESC = new HashMap<>(16);
    private static final Map<Character, Class<?>> DESC_MAP_PRIMITIVE = new HashMap<>(16);

    private static final ConcurrentMap<String, Class<?>> DESC_MAP_CLASS_CACHE = new ConcurrentHashMap<>();


    static {
        PRIMITIVE_MAP_DESC.put(void.class, 'V');
        PRIMITIVE_MAP_DESC.put(boolean.class, 'Z');
        PRIMITIVE_MAP_DESC.put(byte.class, 'B');
        PRIMITIVE_MAP_DESC.put(char.class, 'C');
        PRIMITIVE_MAP_DESC.put(double.class, 'D');
        PRIMITIVE_MAP_DESC.put(float.class, 'F');
        PRIMITIVE_MAP_DESC.put(int.class, 'I');
        PRIMITIVE_MAP_DESC.put(long.class, 'J');
        PRIMITIVE_MAP_DESC.put(short.class, 'S');

        DESC_MAP_PRIMITIVE.put('V', void.class);
        DESC_MAP_PRIMITIVE.put('Z', boolean.class);
        DESC_MAP_PRIMITIVE.put('B', byte.class);
        DESC_MAP_PRIMITIVE.put('C', char.class);
        DESC_MAP_PRIMITIVE.put('D', double.class);
        DESC_MAP_PRIMITIVE.put('F', float.class);
        DESC_MAP_PRIMITIVE.put('I', int.class);
        DESC_MAP_PRIMITIVE.put('J', long.class);
        DESC_MAP_PRIMITIVE.put('S', short.class);
    }

    /**
     * 生成参数字节码描述符，用于网络通信使用
     *
     * @param c
     * @return
     */
    public static String getDesc(Class<?> c) {
        StringBuilder ret = new StringBuilder();

        while (c.isArray()) {
            ret.append('[');
            c = c.getComponentType();
        }

        if (c.isPrimitive()) {
            ret.append(PRIMITIVE_MAP_DESC.get(c));
        } else {
            ret.append('L');
            ret.append(c.getName().replace('.', '/'));
            ret.append(';');
        }
        return ret.toString();
    }

    public static Class<?> desc2class(String desc) throws ClassNotFoundException {

        Character d = desc.charAt(0);
        Class<?> primitive = DESC_MAP_PRIMITIVE.get(d);
        // 如果是原始类型，直接返回即可
        if (Objects.nonNull(primitive)) {
            return primitive;
        }
        // 表示对象，比如 Ljava/lang/Object;
        if ('L' == d) {
            desc = desc.substring(1, desc.length() - 1)
                .replace('/', '.');
            // 表示数组, [Ljava/lang/Object; => Ljava.lang.Object;
        } else if ('[' == d) {
            desc = desc.replace('/', '.');
        } else {
            // 不认识的字节码描述符好
            throw new ClassNotFoundException("Class not found: " + desc);
        }

        Class<?> clazz = DESC_MAP_CLASS_CACHE.get(desc);
        if (Objects.isNull(clazz)) {
            // 获取类加载器
            ClassLoader classLoader = DescClassUtils.getClassLoader(DescClassUtils.class);
            clazz = Class.forName(desc, true, classLoader);
            DESC_MAP_CLASS_CACHE.put(desc, clazz);
        }
        return clazz;
    }

    public static ClassLoader getClassLoader(Class<?> cls) {
        return CommonUtils.getClassLoader(cls);
    }
}
