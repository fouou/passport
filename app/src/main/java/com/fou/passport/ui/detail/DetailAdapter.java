package com.fou.passport.ui.detail;

import static com.fou.passport.until.Type.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fou.passport.R;
import com.fou.passport.room.DatasetAndMetadata;
import com.fou.passport.room.DatasetEntity;
import com.fou.passport.room.MetadataEntity;
import com.fou.passport.until.DpToPx;
import com.fou.passport.until.MyUntil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 详情页账户卡片列表：每个 dataset 对应一张卡片
 */
public class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.DetailHolder> {

    public interface OnCardActionListener {
        void onCardClick(DatasetEntity datasetEntity, int position);

        void onCardLongClick(View cardView, DatasetEntity datasetEntity, int position);
    }

    private final List<DatasetAndMetadata> datasetAndMetadataList;
    private final OnCardActionListener listener;
    private boolean isShowPassword = false;

    public DetailAdapter(List<DatasetAndMetadata> datasetAndMetadataList, OnCardActionListener listener) {
        this.datasetAndMetadataList = datasetAndMetadataList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DetailHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        //cardView
        MaterialCardView materialCardView = new MaterialCardView(context, null, com.google.android.material.R.attr.materialCardViewElevatedStyle);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        params.setMargins(DpToPx.dpToPx(10), DpToPx.dpToPx(20), DpToPx.dpToPx(10), DpToPx.dpToPx(20));
        materialCardView.setLayoutParams(params);
        // 设置LinearLayout
        LinearLayout linearLayout_container_metadata = new LinearLayout(context);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(DpToPx.dpToPx(20), DpToPx.dpToPx(20), DpToPx.dpToPx(20), DpToPx.dpToPx(20));
        linearLayout_container_metadata.setLayoutParams(layoutParams);
        linearLayout_container_metadata.setOrientation(LinearLayout.VERTICAL);
        //添加标题（Account 1）
        MaterialTextView materialTextView = new MaterialTextView(context);
        materialTextView.setTextAppearance(R.style.PassportSectionTitle);
        LinearLayout.LayoutParams params_text = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params_text.setMargins(0, 0, 0, DpToPx.dpToPx(10));
        materialTextView.setLayoutParams(params_text);
        //addView
        linearLayout_container_metadata.addView(materialTextView);
        materialCardView.addView(linearLayout_container_metadata);
        materialCardView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.scale_up));
        return new DetailHolder(materialCardView, linearLayout_container_metadata, materialTextView);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailHolder holder, int position) {
        DatasetAndMetadata datasetAndMetadata = datasetAndMetadataList.get(position);
        holder.materialTextView.setText(holder.materialTextView.getContext().getString(R.string.account_count, position + 1));
        //复用时先清理旧的字段行，仅保留标题
        holder.linearLayout_container_metadata.removeViews(1, holder.linearLayout_container_metadata.getChildCount() - 1);
        extracted(datasetAndMetadata, holder.linearLayout_container_metadata);
        //长按事件
        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return false;
            }
            listener.onCardLongClick(v, datasetAndMetadataList.get(adapterPosition).datasetEntity, adapterPosition);
            return false;
        });
        //点击事件
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            listener.onCardClick(datasetAndMetadataList.get(adapterPosition).datasetEntity, adapterPosition);
        });
    }

    @Override
    public int getItemCount() {
        return datasetAndMetadataList.size();
    }

    /**
     * 移除已删除的账户卡片
     *
     * @param position
     */
    public void removeItem(int position) {
        if (position < 0 || position >= datasetAndMetadataList.size()) {
            return;
        }
        datasetAndMetadataList.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Add item in the container in order(stringList)
     *
     * @param datasetAndMetadata
     */
    private void extracted(DatasetAndMetadata datasetAndMetadata, LinearLayout datasetContainer) {
        for (String s : allStrings) {//按照allString列表顺序展示
            for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
                if (s.equals(metadataEntity.key_word) && metadataEntity.auto_fill_value_text != null) {
                    addItemToContainer(s, metadataEntity.auto_fill_value_text, datasetContainer);
                }
            }
        }
        //添加模板字段
        addCategory(datasetAndMetadata, datasetContainer);
    }

    /**
     * 添加模板字段
     *
     * @param datasetAndMetadata
     * @param datasetContainer
     */
    private void addCategory(DatasetAndMetadata datasetAndMetadata, LinearLayout datasetContainer) {
        Set<String> categorySet = new HashSet<>();//防止重复
        for (MetadataEntity metadataEntity : datasetAndMetadata.metadataEntityList) {
            if (metadataEntity.key_word != null) {
                switch (metadataEntity.key_word) {
                    case CARD:
                        categorySet.add(datasetContainer.getContext().getString(R.string.bank_cards));
                        break;
                    case PASSWORD:
                        categorySet.add(datasetContainer.getContext().getString(R.string.passwords));
                        break;
                    // TODO 许可证模板
                }
            }
        }
        if (!categorySet.isEmpty()) {
            String category = MyUntil.turnSetToString(categorySet);
            addItemToContainer(OTHER_CATEGORY, category, datasetContainer);
        }
    }

    /**
     * @param key_word
     * @param text
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void addItemToContainer(String key_word, String text, LinearLayout datasetContainer) {
        Context context = datasetContainer.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_detail_layout, datasetContainer, false);
        ShapeableImageView imageView = view.findViewById(R.id.item_imageView);
        MaterialTextView textView = view.findViewById(R.id.item_textView);
        MaterialTextView textView_right = view.findViewById(R.id.item_textView_right);
        switch (key_word) {
            case USERNAME:
                textView.setText(R.string.username);
                imageView.setImageDrawable(context.getDrawable(R.drawable.account_circle_fill0_wght700_grad0_opsz48));
                break;
            case PHONE:
                textView.setText(R.string.phone);
                imageView.setImageDrawable(context.getDrawable(R.drawable.call_fill0_wght700_grad0_opsz48));
                break;
            case EMAIL:
                textView.setText(R.string.email);
                imageView.setImageDrawable(context.getDrawable(R.drawable.mail_fill0_wght700_grad0_opsz48));
                break;
            case PASSWORD:
                textView.setText(R.string.passwords);
                imageView.setImageDrawable(context.getDrawable(R.drawable.key_fill0_wght700_grad0_opsz48));
                addEyeView(view);//小眼睛
                break;
            case CARD:
                textView.setText(R.string.bank_cards);
                imageView.setImageDrawable(context.getDrawable(R.drawable.credit_card_fill0_wght700_grad0_opsz48));
                break;
            case CVV:
                textView.setText(R.string.cvv);
                imageView.setImageDrawable(context.getDrawable(R.drawable.security_fill0_wght700_grad0_opsz48__1_));
                break;
            case OTHER_WEBSITE:
                textView.setText(R.string.website);
                imageView.setImageDrawable(context.getDrawable(R.drawable.language_fill0_wght700_grad0_opsz48));
                break;
            case OTHER_APPLICATION:
                textView.setText(R.string.app);
                imageView.setImageDrawable(context.getDrawable(R.drawable.crop_square));
                break;
            case OTHER_CREATE_TIME:
                imageView.setImageDrawable(context.getDrawable(R.drawable.schedule));
                textView.setText(R.string.creation_time);
                textView_right.setText(MyUntil.getSaveTime(text));//需要将毫秒转对应语言的时间日期
                datasetContainer.addView(view);
                return;
            case OTHER_CATEGORY:
                imageView.setImageDrawable(context.getDrawable(R.drawable.sell_fill0_wght700_grad0_opsz48));
                textView.setText(R.string.category);
                break;
            case OTHER_ISSUING_BANK:
                imageView.setImageDrawable(context.getDrawable(R.drawable.location_city_fill0_wght700_grad0_opsz48));
                textView.setText(R.string.issuing_bank);
                break;
        }
        textView_right.setText(text);
        datasetContainer.addView(view);
    }

    private void addEyeView(View view) {
        RelativeLayout relativeLayout = (RelativeLayout) view;
//addImageButton
        MaterialButton myButton = new MaterialButton(view.getContext(), null, com.google.android.material.R.attr.materialIconButtonStyle);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        myButton.setLayoutParams(params);
        myButton.setIconResource(R.drawable.visibility_off_fill0_wght700_grad0_opsz48);
        //添加到relativeLayout
        relativeLayout.addView(myButton);
        //默认不可见
        MaterialTextView materialTextView = view.findViewById(R.id.item_textView_right);
        materialTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        myButton.setOnClickListener(v -> showHidePassword(materialTextView, myButton));
    }

    private void showHidePassword(MaterialTextView textView, MaterialButton button) {
        if (isShowPassword) {
            // 显示星号
            textView.setTransformationMethod(PasswordTransformationMethod.getInstance());
            button.setIconResource(R.drawable.visibility_off_fill0_wght700_grad0_opsz48);
            isShowPassword = false;
        } else {
            // 显示明文
            textView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            button.setIconResource(R.drawable.visibility_fill0_wght700_grad0_opsz48);
            isShowPassword = true;
        }
    }

    static class DetailHolder extends RecyclerView.ViewHolder {
        final LinearLayout linearLayout_container_metadata;
        final MaterialTextView materialTextView;

        DetailHolder(@NonNull View itemView, LinearLayout linearLayout_container_metadata, MaterialTextView materialTextView) {
            super(itemView);
            this.linearLayout_container_metadata = linearLayout_container_metadata;
            this.materialTextView = materialTextView;
        }
    }
}
