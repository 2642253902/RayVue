package org.example.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Consumer;

public interface BeansData {


    /*
     * 把当前对象转换成指定的 VO / DTO 对象，并在转换后执行 consumer
     *     * 使用方式示例：
     * Account 实现了 BeansData 后，可以调用 account.asViewOf(AuthorizeVO.class, vo -> {
     *     // 在这里可以对 vo 做一些额外的处理，比如设置某些字段的值
     *     vo.setSomeField("someValue");
     * });
     */
    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer) {
        V v = this.asViewObject(clazz);
        // 在转换后执行 consumer，允许对 VO 做额外处理
        consumer.accept(v);
        return v;
    }

    //把当前对象转换成指定的 VO / DTO 对象
    default <V> V asViewObject(Class<V> clazz) {
        try {
            // 目标类里声明了哪些字段，就尝试从当前对象里找同名字段来复制
            Field[] declaredFields = clazz.getDeclaredFields();
            // 目标类必须有无参构造方法，否则这里无法通过反射创建对象
            Constructor<V> constructor = clazz.getDeclaredConstructor();
            V v = constructor.newInstance();
            for (Field declaredField : declaredFields) {
                convert(declaredField, v);
            }
            return v;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // 把当前对象转换成指定的 VO / DTO 对象，并在转换后执行 consumer
    private void convert(Field field, Object value) {
        try {
            // 只按“字段名相同”来匹配，不关心 getter / setter
            Field source = this.getClass().getDeclaredField(field.getName());
            // 私有字段默认不能直接读写，打开 accessible 后才能通过反射访问
            field.setAccessible(true);
            source.setAccessible(true);
            // 从当前对象读取 source 字段的值，再写入目标对象 value 的同名字段
            field.set(value, source.get(this));
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
            // 目标字段在当前对象里找不到，或者无法访问时就跳过
            // 这样 VO 可以只定义自己需要暴露的字段，不必和实体类完全一致
        }
    }

}
