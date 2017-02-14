/**
 * Copyright (c) 2016, smuyyh@gmail.com All Rights Reserved.
 * #                                                   #
 * #                       _oo0oo_                     #
 * #                      o8888888o                    #
 * #                      88" . "88                    #
 * #                      (| -_- |)                    #
 * #                      0\  =  /0                    #
 * #                    ___/`---'\___                  #
 * #                  .' \\|     |# '.                 #
 * #                 / \\|||  :  |||# \                #
 * #                / _||||| -:- |||||- \              #
 * #               |   | \\\  -  #/ |   |              #
 * #               | \_|  ''\---/''  |_/ |             #
 * #               \  .-\__  '-'  ___/-. /             #
 * #             ___'. .'  /--.--\  `. .'___           #
 * #          ."" '<  `.___\_<|>_/___.' >' "".         #
 * #         | | :  `- \`.;`\ _ /`;.`/ - ` : | |       #
 * #         \  \ `_.   \_ __\ /__ _/   .-` /  /       #
 * #     =====`-.____`.___ \_____/___.-`___.-'=====    #
 * #                       `=---='                     #
 * #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #
 * #                                                   #
 * #               佛祖保佑         永无BUG             #
 * #                                                   #
 */
package com.justwayward.reader.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.menu.MenuBuilder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.justwayward.reader.R;
import com.justwayward.reader.base.BaseActivity;
import com.justwayward.reader.base.Constant;
import com.justwayward.reader.bean.user.TencentLoginResult;
import com.justwayward.reader.component.AppComponent;
import com.justwayward.reader.component.DaggerMainComponent;
import com.justwayward.reader.service.DownloadBookService;
import com.justwayward.reader.ui.contract.MainContract;
import com.justwayward.reader.ui.fragment.CommunityFragment;
import com.justwayward.reader.ui.fragment.FindFragment;
import com.justwayward.reader.ui.fragment.RecommendFragment;
import com.justwayward.reader.ui.presenter.MainActivityPresenter;
import com.justwayward.reader.utils.LogUtils;
import com.justwayward.reader.utils.SharedPreferencesUtil;
import com.justwayward.reader.utils.ToastUtils;
import com.justwayward.reader.view.LoginPopupWindow;
import com.justwayward.reader.view.RVPIndicator;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;

/**
 * https://github.com/JustWayward/BookReader
 */
public class MainActivity extends BaseActivity implements MainContract.View, LoginPopupWindow.LoginTypeListener {

    @Bind(R.id.indicator)
    RVPIndicator mIndicator;
    @Bind(R.id.viewpager)
    ViewPager mViewPager;

    private List<Fragment> mTabContents;
    private FragmentPagerAdapter mAdapter;
    private List<String> mDatas;

    @Inject
    MainActivityPresenter mPresenter;

    // 退出时间
    private long currentBackPressedTime = 0;
    // 退出间隔
    private static final int BACK_PRESSED_INTERVAL = 2000;

    private LoginPopupWindow popupWindow;
    public static Tencent mTencent;
    public IUiListener loginListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, DownloadBookService.class));//下载服务

        initDatas();
        configViews();

        mTencent = Tencent.createInstance("1105670298", MainActivity.this);
    }

    //引用布局
    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerMainComponent.builder()
                .appComponent(appComponent)
                .build()
                .inject(this);
    }

    @Override
    public void initToolBar() {
        mCommonToolbar.setLogo(R.mipmap.logo);
        setTitle("");
    }

    @Override
    public void initDatas() {
        //赋值
        mDatas = Arrays.asList(getResources().getStringArray(R.array.home_tabs));
        //初始化并添加三个fragment
        mTabContents = new ArrayList<>();
        mTabContents.add(new RecommendFragment());
        mTabContents.add(new CommunityFragment());
        mTabContents.add(new FindFragment());
        //适配器
        mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return mTabContents.size();
            }

            @Override
            public Fragment getItem(int position) {
                return mTabContents.get(position);
            }
        };
    }

    @Override
    public void configViews() {
        mIndicator.setTabItemTitles(mDatas);//将tab的三个标签赋值
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOffscreenPageLimit(3);//tab标签的数量
        mIndicator.setViewPager(mViewPager, 0);

        mPresenter.attachView(this);
    }


    //右上角菜单栏，点击显示
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //右上角菜单栏每一项点击的监听响应
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search://点击头部的搜索，跳转到搜索界面
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
                break;
            case R.id.action_login://点击菜单栏的登录界面
                if (popupWindow == null) {
                    popupWindow = new LoginPopupWindow(this);
                    popupWindow.setLoginTypeListener(this);
                }
                popupWindow.showAtLocation(mCommonToolbar, Gravity.CENTER, 0, 0);
                break;
            case R.id.action_my_message:
                break;
            case R.id.action_sync_bookshelf:
                break;
            case R.id.action_scan_local_book:
                break;
            case R.id.action_wifi_book:
                break;
            case R.id.action_feedback:
                break;
            case R.id.action_night_mode://夜间模式和日间模式的切换按钮
                if (SharedPreferencesUtil.getInstance().getBoolean(Constant.ISNIGHT, false)) {
                    SharedPreferencesUtil.getInstance().putBoolean(Constant.ISNIGHT, false);
                    //这里设置非夜间模式
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    SharedPreferencesUtil.getInstance().putBoolean(Constant.ISNIGHT, true);
                    //这里设置夜间模式
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
//                MODE_NIGHT_NO： 使用亮色(light)主题，不使用夜间模式
//                MODE_NIGHT_YES：使用暗色(dark)主题，使用夜间模式
//                MODE_NIGHT_AUTO：根据当前时间自动切换 亮色(light)/暗色(dark)主题
//                MODE_NIGHT_FOLLOW_SYSTEM(默认选项)：设置为跟随系统，通常为MODE_NIGHT_NO
                recreate();//重启app
                break;
            case R.id.action_settings:
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    //重写返回键，连续按两次返回键即可退出程序
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - currentBackPressedTime > BACK_PRESSED_INTERVAL) {
                currentBackPressedTime = System.currentTimeMillis();
                ToastUtils.showToast(getString(R.string.exit_tips));
                return true;
            } else {
                finish(); // 退出
            }
        }else if(event.getKeyCode() == KeyEvent.KEYCODE_MENU){
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 显示item中的图片；
     * onPrepareOptionsPanel（View v，Menu menu）代替 onMenuOpened() 。
     * 这样的话，toolbar中menu中的图标就可以显示了
     */
    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        if (menu != null) {
            if (menu.getClass() == MenuBuilder.class) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onPrepareOptionsPanel(view, menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, DownloadBookService.class));
    }

    @Override
    public void loginSuccess() {
        ToastUtils.showSingleToast("登陆成功");
    }

    @Override
    public void onLogin(ImageView view, String type) {
        if (type.equals("QQ")) { //qq登录
            if (!mTencent.isSessionValid()) {
                if (loginListener == null) loginListener = new BaseUIListener();
                mTencent.login(this, "all", loginListener);
            }
        }
        //4f45e920ff5d1a0e29d997986cd97181
    }

    @Override
    public void showError() {

    }

    @Override
    public void complete() {

    }


    public class BaseUIListener implements IUiListener {

        @Override
        public void onComplete(Object o) {
            JSONObject jsonObject = (JSONObject) o;
            String json = jsonObject.toString();
            Gson gson = new Gson();
            TencentLoginResult result = gson.fromJson(json, TencentLoginResult.class);
            LogUtils.e(result.toString());
            mPresenter.login(result.openid, result.access_token, "QQ");
        }

        @Override
        public void onError(UiError uiError) {
        }

        @Override
        public void onCancel() {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_LOGIN || requestCode == Constants.REQUEST_APPBAR) {
            Tencent.onActivityResultData(requestCode, resultCode, data, loginListener);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}