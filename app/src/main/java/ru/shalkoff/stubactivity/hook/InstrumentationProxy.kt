package ru.shalkoff.stubactivity.hook

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import ru.shalkoff.stubactivity.activities.StubActivity
import java.lang.reflect.Method

/**
 * Наш прокси-класс для Instrumentation.
 * Он перехватывает вызовы запуска Activity и создания Activity.
 *
 * @param base Оригинальный Instrumentation, который мы сохраняем, чтобы вызывать его методы для штатных ситуаций.
 */
class InstrumentationProxy(private val base: Instrumentation) : Instrumentation() {

    companion object {
        private const val TAG = "InstrumentationProxy"
        // Ключ для сохранения оригинального Intent'а внутри Intent'а-заглушки
        const val EXTRA_TARGET_INTENT = "extra_target_intent"
    }

    init {
        // Копируем все приватные поля из базового Instrumentation в наш Proxy.
        try {
            val fields = Instrumentation::class.java.declaredFields
            for (field in fields) {
                field.isAccessible = true
                val value = field.get(base)
                field.set(this, value)
            }
            Log.d(TAG, "Успешно скопированы внутренние поля (${fields.size} шт.)")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при копировании внутренних полей", e)
        }
    }

    /**
     * Перехватываем метод execStartActivity.
     * Этот метод вызывается системой (через Activity.startActivity) ПЕРЕД тем, как запрос уйдет в AMS (Activity Manager Service).
     *
     * Здесь мы можем подменить Intent.
     */
    fun execStartActivity(
        who: Context,
        contextThread: IBinder,
        token: IBinder?,
        target: Activity?,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ): ActivityResult? {
        Log.d(TAG, "execStartActivity вызван для: ${intent.component}")

        var finalIntent = intent
        
        // Проверяем, знает ли система об этой Activity.
        // Если resolveActivity возвращает null, значит Activity не зарегистрирована в Manifest.
        // Мы также проверяем, что Intent явный (есть component) и относится к нашему пакету.
        val component = intent.component
        val resolveInfo = if (component != null) {
            who.packageManager.resolveActivity(intent, 0)
        } else {
            // Если Intent неявный, мы не вмешиваемся (или можно добавить логику для неявных)
            null 
        }

        // Если компонент наш, но система его не нашла - это наш клиент
        if (component != null && component.packageName == who.packageName && resolveInfo == null) {
            Log.d(TAG, "Обнаружена незарегистрированная Activity: ${component.className}. Перехватываем!")
            
            // Создаем новый Intent, указывающий на StubActivity (которая есть в манифесте)
            finalIntent = Intent(who, StubActivity::class.java)
            // Сохраняем оригинальный Intent (на TargetActivity) как extra, чтобы потом его восстановить
            finalIntent.putExtra(EXTRA_TARGET_INTENT, intent)
        }

        try {
            // Вызываем оригинальный метод execStartActivity через рефлексию.
            // Мы не можем вызвать super.execStartActivity, так как этот метод скрыт (hidden API).
            val execStartActivity: Method = Instrumentation::class.java.getDeclaredMethod(
                "execStartActivity",
                Context::class.java,
                IBinder::class.java,
                IBinder::class.java,
                Activity::class.java,
                Intent::class.java,
                Int::class.javaPrimitiveType,
                Bundle::class.java
            )
            execStartActivity.isAccessible = true
            return execStartActivity.invoke(
                base,
                who,
                contextThread,
                token,
                target,
                finalIntent,
                requestCode,
                options
            ) as? ActivityResult
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при вызове execStartActivity", e)
            throw RuntimeException("Ошибка при вызове execStartActivity", e)
        }
    }

    /**
     * Перехватываем метод newActivity.
     * Этот метод вызывается системой ПОСЛЕ того, как AMS разрешил запуск Activity,
     * и приложение готовится создать Java-объект Activity.
     *
     * Здесь мы можем подменить создаваемый класс Activity.
     */
    override fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity {
        Log.d(TAG, "newActivity вызван для класса: $className")

        // Проверяем, есть ли в Intent наш спрятанный оригинальный Intent
        val targetIntent = intent.getParcelableExtra<Intent>(EXTRA_TARGET_INTENT)
        if (targetIntent != null) {
            val targetClassName = targetIntent.component?.className
            if (targetClassName != null) {
                Log.d(TAG, "Найден скрытый Intent! Восстанавливаем TargetActivity: $targetClassName")
                // Вместо запрошенного класса (StubActivity) создаем экземпляр нашего целевого класса (TargetActivity)
                return super.newActivity(cl, targetClassName, targetIntent)
            }
        }

        // Если подмены не требуется, создаем Activity как обычно
        return base.newActivity(cl, className, intent)
    }
}
