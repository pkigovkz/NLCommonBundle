package kz.gov.pki.knca.gui;

import java.util.Locale;
import java.util.ResourceBundle;

public final class ProgramSettings {
    
    private static ResourceBundle dictionary;
    private Locale[] arrayLocales;
    private ResourceBundle[] arrayLocalesResource;
    private static ProgramSettings instance = null;
    private int defaultLocaleIndex = 0;
    
    /**
     * Конструктор класса
     * 
     */
    private ProgramSettings() {
        arrayLocales = new Locale[3];
        arrayLocales[0] = new Locale("ru");
        arrayLocales[1] = new Locale("kk");
        arrayLocales[2] = new Locale("en");

        int localesCount = arrayLocales.length;

        arrayLocalesResource = new ResourceBundle[localesCount];
        for (int i = 0; i < localesCount; i++) {
            arrayLocalesResource[i] = ResourceBundle.getBundle("i18n.dictionary", arrayLocales[i]);
        }

        updateLanguage(defaultLocaleIndex);
    }
    
    /**
     * Возвращает экземпляр объекта <code>ProgramSettings</code>
     *
     * @return экземпляр объекта
     */
    public static ProgramSettings getInstance() {
        if(instance == null) {
            instance = new ProgramSettings();
        }
        return instance;
    }
    
    /**
     * Возвращает строку для данного ключа из пакета ресурсов
     *
     * @param key ключ, для выбора строки
     * @return строка для данного ключа
     */
    public String getDictionary(String key) {
        return dictionary.getString(key);
    }
    
    /**
     * Метод для обновления языка интерфейса
     *
     * @param type индекс языкового пакета
     */
    public void updateLanguage(int type) {
        dictionary = arrayLocalesResource[type];
    }
    
    /**
     * Функция для установки языка интерфейса
     *
     * @param language язык для установки
     */
    public void setLanguage(String language) {
        if (language == null || language.isEmpty()) {
            updateLanguage(defaultLocaleIndex);
            return;
        }
        for (int i = 0; i < arrayLocales.length; i++) {
            if (arrayLocales[i].getLanguage().equals(language)) {
                updateLanguage(i);
                return;
            }
        }
    }
}
