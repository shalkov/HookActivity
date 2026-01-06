package ru.shalkoff.stubactivity.hook

import android.app.Instrumentation
import android.util.Log

object HookManager {
    private const val TAG = "HookManager"

    /**
     * Применяет глобальный хук к ActivityThread.
     * Это повлияет на все НОВЫЕ Activity, которые будут запущены в этом процессе.
     */
    fun applyHook() {
        try {
            Log.d(TAG, "Применяем глобальный хук Instrumentation...")

            // 1. Получаем класс ActivityThread (это приватный класс Android)
            val activityThreadClass = Class.forName("android.app.ActivityThread")

            // 2. Получаем статический метод currentActivityThread, чтобы достать текущий экземпляр ActivityThread
            val currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentActivityThreadMethod.isAccessible = true
            val currentActivityThread = currentActivityThreadMethod.invoke(null)

            // 3. Получаем поле mInstrumentation из ActivityThread
            val mInstrumentationField = activityThreadClass.getDeclaredField("mInstrumentation")
            mInstrumentationField.isAccessible = true

            // 4. Берем текущий (оригинальный) Instrumentation
            val originalInstrumentation = mInstrumentationField.get(currentActivityThread) as Instrumentation

            // 5. Проверяем, не применили ли мы уже хук, чтобы не создавать матрешку
            if (originalInstrumentation is InstrumentationProxy) {
                Log.d(TAG, "Глобальный хук уже установлен!")
                return
            }

            // 6. Создаем наш Proxy, оборачивая оригинальный Instrumentation
            val proxyInstrumentation = InstrumentationProxy(originalInstrumentation)

            // 7. Подменяем поле mInstrumentation в ActivityThread на наш InstrumentationProxy
            mInstrumentationField.set(currentActivityThread, proxyInstrumentation)

            Log.d(TAG, "Глобальный хук успешно установлен!")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при установке глобального хука", e)
        }
    }
}
