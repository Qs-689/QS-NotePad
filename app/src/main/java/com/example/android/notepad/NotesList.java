/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    // 添加搜索状态变量
    private String mCurrentSearchQuery = null;
    private String mCurrentCategoryFilter = null;
    private String mCurrentTheme = "light";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2
            NotePad.Notes.COLUMN_NAME_CATEGORY //3
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2;
    private static final int COLUMN_INDEX_CATEGORY = 3;

    // 修改列表项布局，在时间戳后面显示分类


    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 应用当前主题
        applyTheme();
        // 设置列表背景色
        if (mCurrentTheme.equals("colorful")) {
            getListView().setBackgroundColor(getResources().getColor(R.color.background_colorful));
        }

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);


        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView,极速快3.
         */
        // 设置列表分割线
        getListView().setDivider(getResources().getDrawable(android.R.color.darker_gray));
        getListView().setDividerHeight(1);

        // 加载数据
        loadData();
    }
    /**
     * 应用主题
     */
    private void applyTheme() {
        // 从SharedPreferences读取保存的主题设置
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        mCurrentTheme = prefs.getString("theme", "light");

        if (mCurrentTheme.equals("dark")) {
            setTheme(R.style.AppTheme_Dark);
        } else if (mCurrentTheme.equals("colorful")) { // 新增彩色主题
            setTheme(R.style.AppTheme_Colorful);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    /**
     * 加载数据（新增方法）
     */
    private void loadData() {
        String selection = null;
        String[] selectionArgs = null;
        // 构建查询条件
        List<String> selectionParts = new ArrayList<>();
        List<String> selectionArgsList = new ArrayList<>();

        // 添加搜索条件
        if (mCurrentSearchQuery != null && !mCurrentSearchQuery.isEmpty()) {
            selectionParts.add("(" + NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                    NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?)");
            selectionArgsList.add("%" + mCurrentSearchQuery + "%");
            selectionArgsList.add("%" + mCurrentSearchQuery + "%");
        }

        // 添加分类筛选条件
        if (mCurrentCategoryFilter != null && !mCurrentCategoryFilter.isEmpty()) {
            selectionParts.add(NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?");
            selectionArgsList.add(mCurrentCategoryFilter);
        }

        // 组合查询条件
        if (!selectionParts.isEmpty()) {
            selection = TextUtils.join(" AND ", selectionParts);
            selectionArgs = selectionArgsList.toArray(new String[0]);
        }

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         *
         * Please see the introductory note about performing provider operations on the UI thread.
         */
        Cursor cursor = managedQuery(
                getIntent().getData(),            // Use the default content URI for the provider.
                PROJECTION,                       // Return the note ID and title for each note.
                selection,                        // 添加搜索条件
                selectionArgs,                     // 添加搜索参数
                NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
        );

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view, initialized to the title column
        // 修改数据列映射
        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,  // 添加时间戳
                NotePad.Notes.COLUMN_NAME_CATEGORY
        };

        int[] viewIDs = {
                android.R.id.text1,
                android.R.id.text2,  // 映射到第二个TextView
                R.id.text_category
        };

        // 创建适配器
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,  // 使用修改后的布局
                cursor,
                dataColumns,
                viewIDs
        );

        // 设置时间戳格式化
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == COLUMN_INDEX_MODIFICATION_DATE) {
                    // 处理时间戳
                    TextView textView = (TextView) view;
                    long timestamp = cursor.getLong(columnIndex);
                    String formattedTime = formatTimestamp(timestamp);
                    textView.setText(formattedTime);
                    // 根据主题设置文字颜色
                    if (mCurrentTheme.equals("dark")) {
                        textView.setTextColor(getResources().getColor(R.color.text_secondary_dark));
                    } else if (mCurrentTheme.equals("colorful")) {
                        textView.setTextColor(0xFF5D4037); // 彩色主题的棕色文字
                    } else {
                        textView.setTextColor(getResources().getColor(R.color.text_secondary_light));
                    }

                    return true;
                } else if (columnIndex == COLUMN_INDEX_CATEGORY) {
                    // 处理分类显示
                    TextView textView = (TextView) view;
                    String category = cursor.getString(columnIndex);
                    setCategoryView(textView, category);
                    return true;
                }
                return false;
            }
        });

        setListAdapter(adapter);
    }
    /**
     * 设置分类标签的显示
     */
    private void setCategoryView(TextView textView, String category) {
        String displayName = getCategoryDisplayName(category);
        textView.setText(displayName);

        // 根据主题设置分类标签样式
        int color = getCategoryColor(category);
        textView.setBackgroundColor(color);
        textView.setTextColor(Color.WHITE);
        textView.setPadding(8, 4, 8, 4);

        // 设置圆角
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(12);
        shape.setColor(color);
        textView.setBackground(shape);
    }
    private int getCategoryColor(String category) {
        if (category == null) return 0xFF757575; // 默认灰色

        switch (category) {
            case "Work": return 0xFF2196F3;     // 蓝色
            case "Personal": return 0xFF4CAF50; // 绿色
            case "Ideas": return 0xFFFF9800;    // 橙色
            default: return 0xFF757575;         // 灰色
        }
    }

    private String getCategoryDisplayName(String category) {
        if (category == null) return "通用";
        switch (category) {
            case "Work": return "工作";
            case "Personal": return "个人";
            case "Ideas": return "想法";
            default: return "通用";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回时重新加载数据
        loadData();
    }

    private String formatTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "未知时间";
        }
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE极速快3 and the
     * mimeTYpe NotePad.Notes极速快3.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);


        // 添加主题切换子菜单
        SubMenu themeSubMenu = menu.addSubMenu("🎨 主题设置");
        themeSubMenu.getItem().setIcon(android.R.drawable.ic_menu_preferences);
        themeSubMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        // 添加三个主题选项
        MenuItem lightThemeItem = themeSubMenu.add(Menu.NONE, 1001, Menu.NONE, "☀️ 浅色主题");
        MenuItem darkThemeItem = themeSubMenu.add(Menu.NONE, 1002, Menu.NONE, "🌙 深色主题");
        MenuItem colorfulThemeItem = themeSubMenu.add(Menu.NONE, 1003, Menu.NONE, "🌈 彩色主题");

        // 根据当前主题设置选中状态
        switch (mCurrentTheme) {
            case "light":
                lightThemeItem.setChecked(true);
                break;
            case "dark":
                darkThemeItem.setChecked(true);
                break;
            case "colorful":
                colorfulThemeItem.setChecked(true);
                break;
        }

        // 设置单选按钮组效果
        themeSubMenu.setGroupCheckable(Menu.NONE, true, true);

        // 添加搜索功能
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchItem.getActionView();



        // 设置搜索提示
        searchView.setQueryHint("搜索标题或内容");

        // 恢复搜索状态
        if (mCurrentSearchQuery != null) {
            searchView.setQuery(mCurrentSearchQuery, false);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 用户按下搜索键时执行
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 文本变化时实时搜索
                if (newText.isEmpty()) {
                    clearSearch();
                } else {
                    performSearch(newText);
                }
                return true;
            }
        });

        return true;
    }

    /**
     * 执行搜索功能
     */
    private void performSearch(String query) {
        mCurrentSearchQuery = query;
        loadData();

        // 显示搜索结果数量
        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        if (adapter != null && adapter.getCursor() != null) {
            Toast.makeText(this, "找到 " + adapter.getCursor().getCount() + " 条相关笔记",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 清除搜索，显示所有笔记
     */
    private void clearSearch() {
        mCurrentSearchQuery = null;
        loadData();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions极速快3 that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

                     // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent极速快3 with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                    Menu.NONE,                  // A unique item ID is not required.
                    Menu.NONE,                  // The alternatives don't need to be in order.
                    null,                       // The caller's name is not excluded from the group.
                    specifics,                  // These specific options must appear first.
                    intent,                     // These Intent objects map to the options in specifics.
                    Menu.NONE,                  // No flags are required.
                    items                       // The menu items generated from the specifics-to-
                    // Intents mapping
            );
            // If the Edit menu item exists, adds shortcuts for it.
            if (items[0] != null) {

                // Sets the Edit menu item shortcut to numeric "1", letter "e"
                items[0].setShortcut('1', 'e');
            }
        } else {
            // If the list is empty, removes any existing alternative actions from the menu
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent极速快3 out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the NotePad application.
     *
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (item.getItemId() == R.id.menu_paste) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        }else if (item.getItemId()  == R.id.menu_filter) { // 分类筛选菜单
            showCategoryFilterDialog();
            return true;
        } else if (item.getItemId() == 1001) { // 浅色主题
            switchToTheme("light");
            return true;
        } else if (item.getItemId() == 1002) { // 深色主题
            switchToTheme("dark");
            return true;
        } else if (item.getItemId() == 1003) { // 彩色主题
            switchToTheme("colorful");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCategoryFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("按分类筛选");

        final String[] categories = {
                "所有分类",
                "通用",
                "工作",
                "个人",
                "想法"
        };

        builder.setItems(categories, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // 所有分类
                        clearCategoryFilter();
                        break;
                    case 1: // 通用
                        applyCategoryFilter("General");
                        break;
                    case 2: // 工作
                        applyCategoryFilter("Work");
                        break;
                    case 3: // 个人
                        applyCategoryFilter("Personal");
                        break;
                    case 4: // 想法
                        applyCategoryFilter("Ideas");
                        break;
                }
            }
        });

        builder.show();
    }
    private void applyCategoryFilter(String category) {
        mCurrentCategoryFilter = category;

        // 重新加载数据
        loadData();

        // 显示筛选结果
        String displayName = getCategoryDisplayName(category);
        int count = getFilteredNoteCount();

        String message = "找到 " + count + " 条" + displayName + "笔记";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // 更新标题显示当前筛选状态
        updateTitle();
    }
    /**
     * 获取筛选后的笔记数量
     */
    private int getFilteredNoteCount() {
        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        if (adapter != null && adapter.getCursor() != null) {
            return adapter.getCursor().getCount();
        }
        return 0;
    }

    /**
     * 更新标题显示筛选状态
     */
    private void updateTitle() {
        if (mCurrentCategoryFilter != null) {
            String categoryName = getCategoryDisplayName(mCurrentCategoryFilter);
            setTitle("笔记 - " + categoryName);
        } else {
            setTitle("所有笔记");
        }
    }


    /**
     * 清除分类筛选
     */
    private void clearCategoryFilter() {
        mCurrentCategoryFilter = null;
        loadData();
        Toast.makeText(this, "显示所有分类", Toast.LENGTH_SHORT).show();
        // 更新标题显示当前筛选状态
        updateTitle();
    }

    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns极速快3
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item极速快3. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        int id = item.getItemId();
        if (id == R.id.context_open) {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (id == R.id.context_copy) { //BEGIN_INCLUDE(copy)
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // Copies the notes URI to the clipboard. In effect, this copies the note itself
            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                             // label for the clip
                    noteUri));                          // the URI

            // Returns to the caller and skips further processing.
            return true;
            //END_INCLUDE(copy)
        } else if (id == R.id.context_delete) {
            // Deletes the note from the provider by passing in a URI in note ID format.
            // Please see the introductory note about performing provider operations on the
            // UI thread.
            getContentResolver().delete(
                    noteUri,  // The URI of the provider
                    null,     // No where clause is needed, since only a single note ID is being
                    // passed in.
                    null      // No where clause is used, so no where arguments are needed.
            );

            // Returns to the caller and skips further processing.
            return true;
        }
        return super.onContextItemSelected(item);
    }
    /**
     * 切换主题
     */
    private void switchToTheme(String theme) {
        // 如果当前已经是该主题，则不进行切换
        if (mCurrentTheme.equals(theme)) {
            Toast.makeText(this, "当前已是" + getThemeDisplayName(theme) + "主题", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("theme", theme);
        editor.commit();

        mCurrentTheme = theme;

        // 显示切换提示
        String themeName = getThemeDisplayName(theme);
        Toast.makeText(this, "已切换到" + themeName + "主题", Toast.LENGTH_SHORT).show();

        // 平滑切换主题
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 获取主题显示名称
     */
    private String getThemeDisplayName(String theme) {
        switch (theme) {
            case "light": return "浅色";
            case "dark": return "深色";
            case "colorful": return "彩色";
            default: return "浅色";
        }
    }

    /**
     * This method is called when the user clicks a note极速快3 in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}