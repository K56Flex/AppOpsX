package com.zzzmode.appopsx.ui.main;

import android.graphics.Color;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseIntArray;

import com.github.promeg.pinyinhelper.Pinyin;
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.model.AppInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by zl on 2017/1/23.
 */
class SearchHandler {

    private static final String TAG = "SearchHandler";

    private List<AppInfo> mBaseData;

    private RecyclerView recyclerView;
    private SearchResultAdapter mAdapter;

    void setBaseData(List<AppInfo> baseData) {
        this.mBaseData = baseData;
    }

    void initView(RecyclerView recyclerView){
        this.recyclerView=recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new SimpleListDividerDecorator(ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.list_divider_h), true));
        recyclerView.setItemAnimator(new RefactoredDefaultItemAnimator());
        mAdapter=new SearchResultAdapter();
        recyclerView.setAdapter(mAdapter);
    }

    void handleWord(final String text){
        if(TextUtils.isEmpty(text)){
            mAdapter.clear();
            return;
        }

        search(text)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(new ResourceObserver<List<AppInfo>>() {


            @Override
            protected void onStart() {
                super.onStart();
            }

            @Override
            public void onNext(List<AppInfo> value) {

                mAdapter.kw=text;
                mAdapter.showItems(value);
                if(!value.isEmpty()){
                    recyclerView.scrollToPosition(0);
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });


    }


    private Observable<List<AppInfo>> search(final String key){
        return Observable.create(new ObservableOnSubscribe<List<AppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<AppInfo>> e) throws Exception {
                Pattern p = Pattern.compile(".*(?i)("+key+").*");
                List<AppInfo> result=new ArrayList<>();
                for (AppInfo info : mBaseData) {
                    if(p.matcher(info.appName).matches()){
                        result.add(info);
                    }else {
                        if(info.pinyin == null){
                            StringBuilder sb=new StringBuilder();
                            char[] chars = info.appName.toCharArray();
                            for (char aChar : chars) {
                                String s = Pinyin.toPinyin(aChar);
                                if(!TextUtils.isEmpty(s)){
                                    sb.append(s.charAt(0));
                                }
                            }
                            info.pinyin=sb.toString();
                        }
                        if(p.matcher(info.pinyin).matches()){
                            result.add(info);
                        }
                    }
                }
                e.onNext(result);
            }
        });
    }

    private static class SearchResultAdapter extends MainListAdapter{

        String kw;

        private int color= Color.parseColor("#FF4081");

        void clear(){
            appInfos.clear();
            notifyDataSetChanged();
        }

        @Override
        protected CharSequence processText(String name) {
            return resultHighlight(kw,name,color);
        }


        private CharSequence resultHighlight(String key,String text,int color){
            String phantom=text.toLowerCase();
            String k=key!=null?key.toLowerCase():null;

            if(k != null && phantom.contains(k)) {
                int st = 0;
                List<Integer> pos = new ArrayList<>(3);
                while ((st = phantom.indexOf(k, st)) != -1) {
                    pos.add(st);
                    st += key.length();
                }
                SpannableStringBuilder stringBuilder = new SpannableStringBuilder(text);
                if (!pos.isEmpty()) {
                    for (Integer idx : pos) {
                        stringBuilder.setSpan(new ForegroundColorSpan(color), idx, idx + key.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                return stringBuilder;
            }
            return text;
        }
    }
}
