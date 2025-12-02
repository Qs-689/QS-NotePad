# NotePad 扩展
## （一）NoteList界面中笔记条目增加时间戳显示
### 1. 功能要求
- 每个新建笔记都会保存新建时间并显示在列表中
- 修改笔记后自动更新为修改时间
- 时间戳以直观的格式显示在笔记标题下方
### 2. 实现思路
#### 1.布局修改：在笔记列表项中添加新的TextView用于显示时间

#### 2.数据扩展：在数据库查询投影中添加修改时间字段

#### 3.适配器调整：更新SimpleCursorAdapter以绑定时间数据

#### 4.时间格式化：将时间戳转换为易读的日期时间格式

### 3. 技术实现
#### (1) 布局文件修改 - noteslist_item.xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <!-- 原标题TextView -->
    <TextView
        android:id="@android:id/text1"
        android:layout_width="match_parent"
        android:layout_height="?android:attr/listPreferredItemHeight"
        android:textAppearance="?android:attr/textAppearanceLarge"
        android:gravity="center_vertical"
        android:paddingLeft="5dip"
        android:singleLine="true" />
    
    <!-- 添加显示时间的TextView -->
    <TextView
        android:id="@+id/text1_time"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAppearance="?android:attr/textAppearanceSmall"
        android:paddingLeft="5dip"/>
</LinearLayout>

#### (2) 数据投影扩展 - NotesList.java
```Java
private static final String[] PROJECTION = new String[] {
    NotePad.Notes._ID, // 0
    NotePad.Notes.COLUMN_NAME_TITLE, // 1
    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE // 2 - 添加修改时间
};
```

#### (3) 适配器配置更新
```Java
// 数据列与视图ID的映射
String[] dataColumns = { 
    NotePad.Notes.COLUMN_NAME_TITLE, 
    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE 
};

int[] viewIDs = { 
    android.R.id.text1, 
    R.id.text1_time 
};
```

#### (4) 时间格式化处理
在NotePadProvider的insert方法中：

```Java
// 获取当前系统时间（毫秒）
Long now = Long.valueOf(System.currentTimeMillis());
Date date = new Date(now);
SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
String dateTime = format.format(date);

// 如果值映射中不包含创建日期，则设置为当前时间
if (values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE) == false) {
    values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, dateTime);
}

// 如果值映射中不包含修改日期，则设置为当前时间
if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, dateTime);
}
```
在NoteEditor的updateNote方法中：

```java
Long now = Long.valueOf(System.currentTimeMillis());
Date date = new Date(now);
SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
String dateTime = format.format(date);
values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, dateTime);
```
### 4.实现效果界面截图
#### (1)创建/修改笔记后显示创建/修改时间
<img width="1080" height="2400" alt="Screenshot_20251125_194624" src="https://github.com/user-attachments/assets/e7d93676-27d6-42b4-aaa2-ae8c7cb2ce89" />

## （二）添加笔记查询功能（根据标题或内容查询）
### 1. 功能要求
- 在笔记列表界面提供搜索框，支持实时搜索

- 可根据笔记标题或内容进行关键字查询

- 查询结果实时显示在列表中

### 2. 实现思路
#### 1.界面添加：在ActionBar中添加搜索菜单项和搜索框

#### 2.搜索逻辑：实现SearchView的监听器处理搜索输入

#### 3.数据过滤：根据输入内容动态过滤Cursor数据

#### 4.结果显示：更新ListView显示过滤后的结果

### 3. 技术实现
#### (1) 菜单文件添加搜索项 - list_options_menu.xml
```java
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/menu_search"
        android:title="搜索"
        android:icon="@android:drawable/ic_search_category_default"
        android:showAsAction="ifRoom|collapseActionView"
        android:actionViewClass="android.widget.SearchView" />
</menu>
```   
#### (2) 搜索功能实现 - NotesList.java
```java
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.list_options_menu, menu);
    
    // 获取搜索菜单项
    MenuItem searchItem = menu.findItem(R.id.menu_search);
    SearchView searchView = (SearchView) searchItem.getActionView();
    
    // 设置搜索框配置
    searchView.setQueryHint("搜索笔记标题或内容...");
    
    // 搜索文本变化监听
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            // 执行搜索
            performSearch(query);
            return true;
        }
        
        @Override
        public boolean onQueryTextChange(String newText) {
            // 实时搜索
            performSearch(newText);
            return true;
        }
    });
    
    return true;
}

/**
 * 执行搜索功能
 */
private void performSearch(String query) {
    if (query == null || query.trim().isEmpty()) {
        // 如果搜索内容为空，显示所有笔记
        setAdapterWithFilter(null);
    } else {
        // 构建搜索条件
        String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + 
                          NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
        String[] selectionArgs = new String[] { 
            "%" + query + "%", 
            "%" + query + "%" 
        };
        
        setAdapterWithFilter(selection, selectionArgs);
    }
}

/**
 * 根据条件设置适配器
 */
private void setAdapterWithFilter(String selection, String[] selectionArgs) {
    // 重新查询Cursor
    Cursor cursor = managedQuery(
        getIntent().getData(),            // 内容URI
        PROJECTION,                       // 返回列
        selection,                        // 条件语句
        selectionArgs,                    // 条件参数
        NotePad.Notes.DEFAULT_SORT_ORDER  // 排序
    );
    
    // 更新适配器
    String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
    int[] viewIDs = { android.R.id.text1, R.id.text1_time };
    
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        this,                            // Context
        R.layout.noteslist_item,          // 布局文件
        cursor,                          // 数据
        dataColumns,                     // 数据列
        viewIDs                          // 视图ID
    );
    
    setListAdapter(adapter);
}
```
### 4.实现效果界面截图
#### (1)点击搜索按钮进行搜索界面
![qq_pic_merged_1764071692248](https://github.com/user-attachments/assets/b443d3f5-f9ae-4817-ae1c-d9e124d93303)

#### (2)输入搜索内容，显示符合条件（根据标题或内容查询）的笔记
<img width="1080" height="2400" alt="Screenshot_20251125_195604" src="https://github.com/user-attachments/assets/2fa868a0-e5e9-4a37-a342-deb40d173a09" />

#### (3)回删搜素内容至空时，显示所有的笔记
<img width="1080" height="2400" alt="Screenshot_20251125_195024" src="https://github.com/user-attachments/assets/58ce1e25-e74a-4f24-a57f-2efa7763c860" />



## （三）笔记分类功能
### 1. 功能要求
- 支持为笔记添加分类标签

- 可按分类筛选显示笔记

- 提供分类管理界面

### 2. 实现思路
#### 1.数据库扩展：在笔记表中添加分类字段

#### 2.界面增强：在编辑界面添加分类选择功能

#### 3.分类筛选：实现按分类过滤笔记列表

#### 4.分类管理：提供分类的增删改查功能

### 3. 技术实现
#### (1) 数据库扩展 - NotePad.java
```java
public static final class Notes implements BaseColumns {
    // 原有字段...
    public static final String COLUMN_NAME_CATEGORY = "category";
    
    // 更新数据库创建语句
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + NOTES_TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME_TITLE + " TEXT,"
                + COLUMN_NAME_NOTE + " TEXT,"
                + COLUMN_NAME_CREATE_DATE + " INTEGER,"
                + COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                + COLUMN_NAME_CATEGORY + " TEXT"  // 添加分类字段
                + ");");
    }
}
```
#### (2) 分类选择界面 - res/layout/category_dialog.xml
```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="选择分类"
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp" />

    <Spinner
        android:id="@+id/category_spinner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:entries="@array/categories" />

    <EditText
        android:id="@+id/new_category"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:hint="或输入新分类" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="end"
        android:layout_marginTop="16dp">

        <Button
            android:id="@+id/btn_cancel"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="取消" />

        <Button
            android:id="@+id/btn_ok"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="确定"
            android:layout_marginStart="8dp" />

    </LinearLayout>

</LinearLayout>
```
#### (3) 分类数组资源 - res/values/arrays.xml
```java
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="categories">
        <item>未分类</item>
        <item>工作</item>
        <item>学习</item>
        <item>生活</item>
        <item>个人</item>
    </string-array>
</resources>
```
#### (4) 分类功能实现 - NoteEditor.java
```java
// 在编辑界面添加分类选择菜单
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    
    // 添加分类菜单项
    MenuItem categoryItem = menu.add(0, MENU_CATEGORY, 0, "分类");
    categoryItem.setIcon(android.R.drawable.ic_menu_sort_by_size);
    
    return true;
}

// 处理分类菜单点击
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        case MENU_CATEGORY:
            showCategoryDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
    }
}

/**
 * 显示分类选择对话框
 */
private void showCategoryDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.category_dialog, null);
    builder.setView(dialogView);
    
    final Spinner categorySpinner = (Spinner) dialogView.findViewById(R.id.category_spinner);
    final EditText newCategoryEdit = (EditText) dialogView.findViewById(R.id.new_category);
    Button btnCancel = (Button) dialogView.findViewById(R.id.btn_cancel);
    Button btnOk = (Button) dialogView.findViewById(R.id.btn_ok);
    
    final AlertDialog dialog = builder.create();
    
    // 设置当前分类
    String currentCategory = getCurrentCategory();
    if (currentCategory != null) {
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) categorySpinner.getAdapter();
        int position = adapter.getPosition(currentCategory);
        if (position >= 0) {
            categorySpinner.setSelection(position);
        }
    }
    
    btnCancel.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dialog.dismiss();
        }
    });
    
    btnOk.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String selectedCategory;
            if (!newCategoryEdit.getText().toString().trim().isEmpty()) {
                selectedCategory = newCategoryEdit.getText().toString().trim();
            } else {
                selectedCategory = categorySpinner.getSelectedItem().toString();
            }
            
            updateNoteCategory(selectedCategory);
            dialog.dismiss();
        }
    });
    
    dialog.show();
}

/**
 * 更新笔记分类
 */
private void updateNoteCategory(String category) {
    ContentValues values = new ContentValues();
    values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, category);
    
    getContentResolver().update(
        mUri,    // 笔记URI
        values,  // 更新值
        null,    // WHERE条件
        null     // WHERE参数
    );
    
    Toast.makeText(this, "分类已更新: " + category, Toast.LENGTH_SHORT).show();
}
```
#### (5) 按分类筛选 - NotesList.java
```java
// 添加分类筛选菜单
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    
    // 添加分类筛选子菜单
    SubMenu categoryMenu = menu.addSubMenu("分类筛选");
    categoryMenu.add(0, MENU_CATEGORY_ALL, 0, "全部");
    categoryMenu.add(0, MENU_CATEGORY_WORK, 0, "工作");
    categoryMenu.add(0, MENU_CATEGORY_STUDY, 0, "学习");
    categoryMenu.add(0, MENU_CATEGORY_LIFE, 0, "生活");
    categoryMenu.add(0, MENU_CATEGORY_PERSONAL, 0, "个人");
    
    return true;
}

// 处理分类筛选
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    String categoryFilter = null;
    
    switch (item.getItemId()) {
        case MENU_CATEGORY_ALL:
            categoryFilter = null;  // 显示全部
            break;
        case MENU_CATEGORY_WORK:
            categoryFilter = "工作";
            break;
        case MENU_CATEGORY_STUDY:
            categoryFilter = "学习";
            break;
        case MENU_CATEGORY_LIFE:
            categoryFilter = "生活";
            break;
        case MENU_CATEGORY_PERSONAL:
            categoryFilter = "个人";
            break;
    }
    
    if (item.getItemId() >= MENU_CATEGORY_ALL && item.getItemId() <= MENU_CATEGORY_PERSONAL) {
        filterByCategory(categoryFilter);
        return true;
    }
    
    return super.onOptionsItemSelected(item);
}

/**
 * 按分类筛选笔记
 */
private void filterByCategory(String category) {
    String selection = null;
    String[] selectionArgs = null;
    
    if (category != null) {
        selection = NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?";
        selectionArgs = new String[] { category };
    }
    
    // 重新查询并更新列表
    Cursor cursor = managedQuery(
        getIntent().getData(),
        PROJECTION,
        selection,
        selectionArgs,
        NotePad.Notes.DEFAULT_SORT_ORDER
    );
   
}
```
### 4.实现效果界面截图

#### (1)点击右上角菜单按钮并点击分类筛选
![retouch_2025112520091620(1)](https://github.com/user-attachments/assets/780f3026-1767-457d-9c8b-d3f3b5bf5664)
#### (2)分别点击通用/工作/个人/想法
![retouch_2025112611235373](https://github.com/user-attachments/assets/070d6900-b606-4a0a-b1a3-c0f08abe6b45)


#### (3)点击所有分类回到首页
<img width="1080" height="2400" alt="Screenshot_20251125_201314" src="https://github.com/user-attachments/assets/df137251-ad1c-4f5f-8fb0-a41e0accd1ba" />

#### (4)点击右上角搜索按钮并点击分类筛选
![retouch_2025112520154286(1)](https://github.com/user-attachments/assets/bade7967-c17d-4103-a78f-c0b1549475d3)
#### (5)分别点击通用/工作/个人/想法
![retouch_2025112611240876](https://github.com/user-attachments/assets/cfbeb8f9-f179-4199-b818-01f3a5a4dc3f)


#### (6)点击所有分类
<img width="1080" height="2400" alt="Screenshot_20251125_201911" src="https://github.com/user-attachments/assets/0f791114-5614-4ab5-91db-4d28163bbacf" />

#### (7)编写/修改笔记时选择分类
![retouch_2025112520274511(1)(1)](https://github.com/user-attachments/assets/e91d6116-0339-4b9c-b555-6634f0584c30)

## （四）UI美化：主题切换与界面优化
### 1. 功能要求
- 实现浅色和深色双主题切换功能

- 优化笔记列表和编辑界面的视觉效果

- 提供一致的颜色方案和字体设置

- 改善用户交互体验和视觉层次

### 2. 实现思路
#### 2.1 主题系统设计
- 定义两套完整的主题配色方案

- 使用SharedPreferences持久化用户主题选择

- 实现实时主题切换无需重启应用

#### 2.2 视觉设计优化
- 采用Material Design设计语言

- 统一配色方案和间距系统

- 优化字体大小和行高设置

#### 2.3 交互体验提升
- 平滑的主题切换动画

- 直观的视觉反馈

- 一致的操作体验

### 3. 技术实现
#### 3.1 主题资源定义 - res/values/styles.xml
```java
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 浅色主题 -->
    <style name="AppTheme" parent="android:Theme.Holo.Light">
        <item name="android:windowBackground">@color/background_light</item>
        <item name="android:colorBackground">@color/background_light</item>
        <item name="android:textColorPrimary">@color/text_primary_light</item>
        <item name="android:textColorSecondary">@color/text_secondary_light</item>
        <item name="android:actionBarStyle">@style/ActionBarStyle.Light</item>
        <item name="android:listViewStyle">@style/ListViewStyle.Light</item>
    </style>

    <!-- 深色主题 -->
    <style name="AppTheme.Dark" parent="android:Theme.Holo">
        <item name="android:windowBackground">@color/background_dark</item>
        <item name="android:colorBackground">@color/background_dark</item>
        <item name="android:textColorPrimary">@color/text_primary_dark</item>
        <item name="android:textColorSecondary">@color/text_secondary_dark</item>
        <item name="android:actionBarStyle">@style/ActionBarStyle.Dark</item>
        <item name="android:listViewStyle">@style/ListViewStyle.Dark</item>
    </style>

    <!-- ActionBar样式 -->
    <style name="ActionBarStyle.Light" parent="android:Widget.Holo.Light.ActionBar">
        <item name="android:background">@color/primary_color</item>
        <item name="android:titleTextStyle">@style/ActionBarTitleStyle.Light</item>
    </style>

    <style name="ActionBarStyle.Dark" parent="android:Widget.Holo.ActionBar">
        <item name="android:background">@color/primary_dark</item>
        <item name="android:titleTextStyle">@style/ActionBarTitleStyle.Dark</item>
    </style>

    <!-- ActionBar标题样式 -->
    <style name="ActionBarTitleStyle.Light" parent="android:TextAppearance.Holo.Widget.ActionBar.Title">
        <item name="android:textColor">@android:color/white</item>
        <item name="android:textSize">18sp</item>
        <item name="android:textStyle">bold</item>
    </style>

    <style name="ActionBarTitleStyle.Dark" parent="android:TextAppearance.Holo.Widget.ActionBar.Title">
        <item name="android:textColor">@android:color/white</item>
        <item name="android:textSize">18sp</item>
        <item name="android:textStyle">bold</item>
    </style>
</resources>
```

#### 3.2 颜色资源定义 - res/values/colors.xml
```java
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 主色调 -->
    <color name="primary_color">#2196F3</color>
    <color name="primary_dark">#1976D2</color>
    <color name="accent_color">#FF4081</color>

    <!-- 浅色主题颜色 -->
    <color name="background_light">#FAFAFA</color>
    <color name="surface_light">#FFFFFF</color>
    <color name="text_primary_light">#212121</color>
    <color name="text_secondary_light">#757575</color>
    <color name="divider_light">#E0E0E0</color>

    <!-- 深色主题颜色 -->
    <color name="background_dark">#121212</color>
    <color name="surface_dark">#1E1E1E</color>
    <color name="text_primary_dark">#E0E0E0</color>
    <color name="text_secondary_dark">#A0A0A0</color>
    <color name="divider_dark">#373737</color>
</resources>
``` 

#### 3.3 尺寸资源定义 - res/values/dimens.xml
```java
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 间距系统 -->
    <dimen name="padding_small">8dp</dimen>
    <dimen name="padding_medium">16dp</dimen>
    <dimen name="padding_large">24dp</dimen>

    <!-- 圆角半径 -->
    <dimen name="corner_radius_small">4dp</dimen>
    <dimen name="corner_radius_medium">8dp</dimen>
    <dimen name="corner_radius_large">12dp</dimen>

    <!-- 高程阴影 -->
    <dimen name="elevation_small">2dp</dimen>
    <dimen name="elevation_medium">4dp</dimen>
    <dimen name="elevation_large">8dp</dimen>
</resources>
``` 

#### 3.4 主题管理类 - ThemeManager.java
```java
public class ThemeManager {
    private static final String PREF_THEME = "app_theme";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";
    
    private SharedPreferences preferences;
    
    public ThemeManager(Context context) {
        preferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
    }
    
    public void setTheme(String theme) {
        preferences.edit().putString(PREF_THEME, theme).apply();
    }
    
    public String getCurrentTheme() {
        return preferences.getString(PREF_THEME, THEME_LIGHT);
    }
    
    public void toggleTheme() {
        if (getCurrentTheme().equals(THEME_LIGHT)) {
            setTheme(THEME_DARK);
        } else {
            setTheme(THEME_LIGHT);
        }
    }
    
    public int getThemeResource() {
        return getCurrentTheme().equals(THEME_DARK) ? 
            R.style.AppTheme_Dark : R.style.AppTheme;
    }
}
```

#### 3.5 笔记列表布局优化 - noteslist_item.xml
```java
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/list_item_background"
    android:padding="@dimen/padding_medium"
    android:layout_margin="@dimen/padding_small">

    <!-- 标题行 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <TextView
            android:id="@android:id/text1"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textAppearance="?android:attr/textAppearanceMedium"
            android:textColor="?android:attr/textColorPrimary"
            android:textStyle="bold"
            android:singleLine="true"
            android:ellipsize="end" />

        <!-- 分类标签 -->
        <TextView
            android:id="@+id/text_category"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textAppearance="?android:attr/textAppearanceSmall"
            android:background="@drawable/category_tag_background"
            android:padding="@dimen/padding_small"
            android:textColor="#FFFFFF"
            android:textSize="12sp" />

    </LinearLayout>

    <!-- 时间戳 -->
    <TextView
        android:id="@+id/text1_time"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:textAppearance="?android:attr/textAppearanceSmall"
        android:textColor="?android:attr/textColorSecondary"
        android:singleLine="true" />

</LinearLayout>
```

#### 3.6 列表项背景选择器 - res/drawable/list_item_background.xml
```java
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="?android:attr/colorControlHighlight" />
            <corners android:radius="@dimen/corner_radius_small" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="?android:attr/colorBackground" />
            <stroke android:width="1dp" android:color="?android:attr/colorForeground" android:alpha="0.1" />
            <corners android:radius="@dimen/corner_radius_small" />
        </shape>
    </item>
</selector>
 ``` 
                
#### 3.7 分类标签背景 - res/drawable/category_tag_background.xml
```java
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/primary_color" />
    <corners android:radius="12dp" />
</shape>
```

#### 3.8 笔记编辑界面优化 - note_editor.xml
```java
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="?android:attr/colorBackground">

    <!-- 标题编辑区域 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="@dimen/padding_medium"
        android:background="?android:attr/colorBackground">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="标题:"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="?android:attr/textColorPrimary" />

        <EditText
            android:id="@+id/title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="8dp"
            android:hint="输入笔记标题"
            android:textSize="16sp"
            android:maxLines="1"
            android:singleLine="true"
            android:textColor="?android:attr/textColorPrimary"
            android:background="@drawable/edit_text_background" />

    </LinearLinearLayout>

    <!-- 内容编辑区域 -->
    <com.example.android.notepad.NoteEditor.LinedEditText
        android:id="@+id/note"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="?android:attr/colorBackground"
        android:padding="@dimen/padding_medium"
        android:gravity="top"
        android:textSize="18sp"
        android:textColor="?android:attr/textColorPrimary"
        android:hint="开始输入笔记内容..."
        android:inputType="textMultiLine"
        android:scrollbars="vertical" />

</LinearLayout>
```

#### 3.9 编辑框背景 - res/drawable/edit_text_background.xml
```java
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="?android:attr/colorBackground" />
    <stroke android:width="1dp" android:color="?android:attr/colorForeground" android:alpha="0.2" />
    <corners android:radius="@dimen/corner_radius_small" />
</shape>
```
     
#### 3.10 主题切换实现 - NotesList.java
```java
public class NotesList extends ListActivity {
    private ThemeManager themeManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用主题（必须在super.onCreate之前）
        themeManager = new ThemeManager(this);
        setTheme(themeManager.getThemeResource());
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list);
        
        // 应用界面样式
        applyUIStyles();
    }
    
    private void applyUIStyles() {
        // 设置列表样式
        getListView().setDivider(getResources().getDrawable(android.R.color.transparent));
        getListView().setDividerHeight(0);
        
        // 设置滚动条样式
        getListView().setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        // 添加主题切换菜单项
        MenuItem themeItem = menu.add(Menu.NONE, 1001, Menu.NONE,
            themeManager.getCurrentTheme().equals("light") ? 
            "🌙 切换到深色模式" : "☀️ 切换到浅色模式");
        themeItem.setIcon(android.R.drawable.ic_menu_preferences);
        themeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1001) {
            // 切换主题
            themeManager.toggleTheme();
            
            // 显示提示
            Toast.makeText(this, 
                themeManager.getCurrentTheme().equals("light") ? 
                "已切换到浅色模式" : "已切换到深色模式", 
                Toast.LENGTH_SHORT).show();
            
            // 重新创建Activity应用新主题
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
```

### 4. 实现效果界面截图
#### 4.1 浅色主题效果
<img width="1080" height="2400" alt="Screenshot_20251126_105212" src="https://github.com/user-attachments/assets/2fa98ba1-7ba4-461d-946f-52a99d50ed1b" />

#### 4.1.1 浅色主题搜索效果
<img width="1080" height="2400" alt="Screenshot_20251126_105327" src="https://github.com/user-attachments/assets/8a088d43-be3e-4f27-9137-a90f1b2eada8" />

#### 4.1.2 主题切换
![retouch_2025112611292314(1)](https://github.com/user-attachments/assets/0903aa87-acc5-4211-bf7a-838a73b39d8c)


#### 4.2 深色主题效果
<img width="1080" height="2400" alt="Screenshot_20251126_105155" src="https://github.com/user-attachments/assets/97952ddb-35b3-4e04-86a0-9866bb2a543b" />

#### 4.2.1 深色主题搜索效果
<img width="1080" height="2400" alt="Screenshot_20251126_105413" src="https://github.com/user-attachments/assets/41c827cd-d5c9-4b9c-86e7-2b2972472ec5" />

#### 4.3 编辑笔记调整字体效果
![retouch_2025112611005645(1)](https://github.com/user-attachments/assets/50475f04-6f85-4cae-af4f-0d1cbb375221)

#### 4.3.1 分别点击大/中/小号字体
![retouch_2025112611032444(1)](https://github.com/user-attachments/assets/fda10e3a-3249-4a83-ac1e-ec5d46e6ce6b)

#### 4.4 美化前后对比
![retouch_2025112611142380](https://github.com/user-attachments/assets/67474203-c626-4e25-a3c6-8bbea3446b96)




















