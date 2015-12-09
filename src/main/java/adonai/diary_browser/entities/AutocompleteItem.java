package adonai.diary_browser.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Класс-отражение БД таблицы autocomplete на объектную модель.
 * <p/>Столбцы таблицы здесь имеют специфические имена из-за обратной совместимости
 * 
 * Created by Adonai on 08.12.15.
 */
@DatabaseTable(tableName = "autocomplete")
public class AutocompleteItem {

    public enum AutocompleteType {
        URL,
        TITLE,
        THEME,
        MUSIC,
        MOOD
    }
    
    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;
    
    @DatabaseField(columnName = "TYPE", dataType = DataType.ENUM_INTEGER)
    private AutocompleteType type;
    
    @DatabaseField(columnName = "TEXT", uniqueIndex = true)
    private AutocompleteType text;
    
    @DatabaseField(columnName = "TITLE")
    private AutocompleteType title;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AutocompleteType getType() {
        return type;
    }

    public void setType(AutocompleteType type) {
        this.type = type;
    }

    public AutocompleteType getText() {
        return text;
    }

    public void setText(AutocompleteType text) {
        this.text = text;
    }

    public AutocompleteType getTitle() {
        return title;
    }

    public void setTitle(AutocompleteType title) {
        this.title = title;
    }
}
