package com.zjw.sdkdemo.function.album_dial.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.function.album_dial.model.BackgroundSelectModel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BackgroundAdapter extends BaseQuickAdapter<BackgroundSelectModel, BaseViewHolder> {
    public BackgroundAdapter(@Nullable List<BackgroundSelectModel> data) {
        super(R.layout.item_background, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, BackgroundSelectModel selectModel) {
        ImageView imageView = baseViewHolder.getView(R.id.background_view);
        LinearLayout defaultLayout = baseViewHolder.getView(R.id.default_add);
        if (selectModel.getResId() != 0) {
            imageView.setVisibility(View.VISIBLE);
            defaultLayout.setVisibility(View.GONE);
            Glide.with(getContext()).load(selectModel.getResId()).into(imageView);
        } else if (selectModel.getBitmap() != null) {
            imageView.setVisibility(View.VISIBLE);
            defaultLayout.setVisibility(View.GONE);
            Glide.with(getContext()).load(selectModel.getBitmap()).into(imageView);
        } else {
            imageView.setVisibility(View.GONE);
            defaultLayout.setVisibility(View.VISIBLE);
            Glide.with(getContext()).load(AppCompatResources.getDrawable(getContext(), R.drawable.baseline_add))
                    .centerInside().into(imageView);
        }

        View view = baseViewHolder.getView(R.id.background_content);
        if (selectModel.isSelected()) {
            view.setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.bg_select_rectangle));
        } else {
            view.setBackground(null);
        }
    }
}
