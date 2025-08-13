package com.zjw.sdkdemo.function.album_dial.adapter;

import android.graphics.Color;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.zhapp.ble.custom.colckvff.ImageUtils;
import com.zhapp.ble.custom.colckvff.PlacementSelectModel;
import com.zjw.sdkdemo.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlacementAdapter extends BaseQuickAdapter<PlacementSelectModel, BaseViewHolder> {
    public PlacementAdapter(@Nullable List<PlacementSelectModel> data) {
        super(R.layout.item_placement, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, PlacementSelectModel selectModel) {
        ImageView imageView = baseViewHolder.getView(R.id.content_view);
        int color = Color.parseColor(selectModel.getColor());
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        imageView.setImageBitmap(ImageUtils.INSTANCE.composeImage(selectModel.getPlacement(), red, green, blue, selectModel.getBackground()));

        ConstraintLayout view = baseViewHolder.getView(R.id.background_content);
        if (selectModel.isSelected()) {
            view.setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.bg_select_rectangle));
        } else {
            view.setBackground(null);
        }
    }
}
