/*
 * Copyright (C) 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * Copyright (C) 2019 Gavriel Fleischer <flocsy@gmail.com>
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
 * limitations under the License
 */

package com.appunite.socket.rxjava2;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appunite.detector.ChangesDetector;
import com.appunite.detector.SimpleDetector;
import com.appunite.socket.R;
import com.google.common.collect.ImmutableList;
import com.jakewharton.rxbinding2.view.RxView;

import java.text.DateFormat;
import java.util.Date;

import javax.annotation.Nonnull;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class BaseViewHolder extends RecyclerView.ViewHolder {

    public BaseViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void bind(@Nonnull MainPresenter.AdapterItem item);

    public abstract void recycle();
}

public class MainAdapter extends RecyclerView.Adapter<BaseViewHolder> implements
        Consumer<ImmutableList<MainPresenter.AdapterItem>>, ChangesDetector.ChangesAdapter {

    private DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    @Nonnull
    private final ChangesDetector<MainPresenter.AdapterItem, MainPresenter.AdapterItem> changesDetector;
    @Nonnull
    private ImmutableList<MainPresenter.AdapterItem> items = ImmutableList.of();

    public MainAdapter() {
        this.changesDetector = new ChangesDetector<>(new SimpleDetector<MainPresenter.AdapterItem>());
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.main_adapter_item, parent, false);
        return new MainViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void accept(@Nonnull ImmutableList<MainPresenter.AdapterItem> items) {
        this.items = items;
        changesDetector.newData(this, items, false);
    }

    private class MainViewHolder extends BaseViewHolder {

        @Nonnull
        private final TextView text;
        @Nonnull
        private final TextView date;
        @Nonnull
        private final TextView details;
        private Disposable disposable;

        public MainViewHolder(@Nonnull View itemView) {
            super(itemView);
            text = checkNotNull((TextView) itemView.findViewById(R.id.main_adapter_item_text));
            date = checkNotNull((TextView) itemView.findViewById(R.id.main_adapter_item_date));
            details = checkNotNull((TextView) itemView.findViewById(R.id.main_adapter_item_details));
        }

        @Override
        public void bind(@Nonnull MainPresenter.AdapterItem item) {
            text.setText(item.text());
            date.setText(timeInstance.format(new Date(item.publishTime())));
            details.setText(item.details());
            details.setVisibility(item.details() == null ? View.GONE : View.VISIBLE);
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            disposable = RxView.clicks(text).subscribe(item.clickObserver());
        }

        @Override
        public void recycle() {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }

    }

}
