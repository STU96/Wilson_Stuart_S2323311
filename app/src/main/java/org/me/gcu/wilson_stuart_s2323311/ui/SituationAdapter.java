package org.me.gcu.wilson_stuart_s2323311.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.me.gcu.wilson_stuart_s2323311.R;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;

import java.util.List;
import java.util.Locale;

// Custom adapter for the lists - shows the icon, type, description and colour for each situation
public class SituationAdapter extends ArrayAdapter<Situation> {

    private final LayoutInflater inflater;
    private final boolean showDistance;

    public SituationAdapter(@NonNull Context context, @NonNull List<Situation> items, boolean showDistance) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
        this.showDistance = showDistance;
    }

    // ViewHolder so findViewById only runs once per row
    private static class ViewHolder {
        View colourStrip;
        ImageView icon;
        TextView type;
        TextView description;
        TextView badge;
        TextView details;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_situation, parent, false);
            holder = new ViewHolder();
            holder.colourStrip = convertView.findViewById(R.id.colourStrip);
            holder.icon = convertView.findViewById(R.id.ivIcon);
            holder.type = convertView.findViewById(R.id.tvType);
            holder.description = convertView.findViewById(R.id.tvDescription);
            holder.badge = convertView.findViewById(R.id.tvBadge);
            holder.details = convertView.findViewById(R.id.tvDetails);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Situation situation = getItem(position);
        if (situation == null) {
            return convertView;
        }
        int colour = SituationStyle.getColour(getContext(), situation);

        holder.colourStrip.setBackgroundColor(colour);
        holder.icon.setImageResource(SituationStyle.getIconRes(situation.getKind()));
        holder.icon.setBackgroundTintList(ColorStateList.valueOf(colour));
        holder.icon.setContentDescription(situation.getTypeLabel());
        holder.type.setText(situation.getTypeLabel());
        holder.description.setText(situation.getDescription());
        holder.badge.setText(SituationStyle.getBadgeText(situation));
        holder.badge.setBackgroundTintList(ColorStateList.valueOf(colour));

        StringBuilder details = new StringBuilder(situation.getRoadName());
        details.append(" · ").append(situation.getLocation().getDirectionText());
        if (showDistance && situation.getDistanceKm() >= 0) {
            details.append(String.format(Locale.UK, " · %.1f km away", situation.getDistanceKm()));
        }
        holder.details.setText(details.toString());

        return convertView;
    }
}
