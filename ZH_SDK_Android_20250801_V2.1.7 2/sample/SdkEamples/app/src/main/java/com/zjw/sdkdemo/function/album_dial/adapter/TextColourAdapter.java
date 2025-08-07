package com.zjw.sdkdemo.function.album_dial.adapter;

import android.graphics.Color;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.function.album_dial.model.ColorSelectModel;
import com.zjw.sdkdemo.function.album_dial.view.PointView;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public class TextColourAdapter extends BaseQuickAdapter<ColorSelectModel, BaseViewHolder> {
    public TextColourAdapter(@Nullable List<ColorSelectModel> data) {
        super(R.layout.item_color, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, ColorSelectModel colorSelectModel) {
        PointView pointView = baseViewHolder.getView(R.id.color_view);
        pointView.setPointColor(Color.parseColor(colorSelectModel.getColor()));

        View view = baseViewHolder.getView(R.id.content);
        if (colorSelectModel.isSelected()) {
            view.setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.bg_select_circle));
        } else {
            view.setBackground(null);
        }
    }
}
