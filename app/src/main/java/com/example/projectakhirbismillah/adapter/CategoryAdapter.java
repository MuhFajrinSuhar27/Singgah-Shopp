package com.example.projectakhirbismillah.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.model.CategoryItem;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<CategoryItem> categoryItems;
    private Context context;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0; // Default: first item (All) selected

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryId);
    }

    public CategoryAdapter(Context context, List<CategoryItem> categoryItems, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryItems = categoryItems;
        this.listener = listener;

        // Set first item as selected
        if (categoryItems.size() > 0) {
            categoryItems.get(0).setSelected(true);
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem item = categoryItems.get(position);
        holder.categoryName.setText(item.getName());
        holder.categoryIcon.setImageResource(item.getIconResource());

        // Set selected state
        if (item.isSelected()) {
            holder.categoryCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent_color));
            // Untuk icon yang terpilih, gunakan warna putih agar kontras dengan background
            holder.categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.white));
            holder.categoryName.setTextColor(ContextCompat.getColor(context, R.color.accent_color));
        } else {
            holder.categoryCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            // HAPUS color filter untuk icon yang tidak terpilih, agar warna asli tampil
            holder.categoryIcon.setColorFilter(null);
            holder.categoryName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }

        // Set click listener
        holder.categoryCard.setOnClickListener(v -> {
            // Update selection
            if (selectedPosition != holder.getAdapterPosition()) {
                // Deselect previous
                categoryItems.get(selectedPosition).setSelected(false);
                notifyItemChanged(selectedPosition);

                // Select new
                selectedPosition = holder.getAdapterPosition();
                item.setSelected(true);
                notifyItemChanged(selectedPosition);

                // Notify fragment about category change
                listener.onCategoryClick(item.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryItems.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        CardView categoryCard;
        ImageView categoryIcon;
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryCard = itemView.findViewById(R.id.category_card);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            categoryName = itemView.findViewById(R.id.category_name);
        }
    }
}