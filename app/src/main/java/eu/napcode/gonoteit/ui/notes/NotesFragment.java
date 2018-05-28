package eu.napcode.gonoteit.ui.notes;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import eu.napcode.gonoteit.R;
import eu.napcode.gonoteit.databinding.FragmentBoardBinding;
import eu.napcode.gonoteit.di.modules.viewmodel.ViewModelFactory;
import eu.napcode.gonoteit.model.note.NoteModel;
import eu.napcode.gonoteit.repository.Resource;
import eu.napcode.gonoteit.repository.Resource.Status;
import eu.napcode.gonoteit.ui.create.CreateActivity;
import eu.napcode.gonoteit.ui.note.NoteActivity;

public class NotesFragment extends Fragment implements NotesAdapter.NoteListener {

    @Inject
    ViewModelFactory viewModelFactory;

    private FragmentBoardBinding binding;

    private NotesViewModel viewModel;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        AndroidSupportInjection.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.viewModel = ViewModelProviders
                .of(this, this.viewModelFactory)
                .get(NotesViewModel.class);

        setupViews();
      }

    @Override
    public void onResume() {
        super.onResume();

        this.viewModel.getNotes().observe(this, this::processNotesResponse);
    }

    private void processNotesResponse(Resource<List<NoteModel>> listResource) {
        boolean loading = listResource.status == Status.LOADING;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (listResource.status == Status.SUCCESS) {
            binding.recyclerView.setAdapter(new NotesAdapter(listResource.data, this));
        }

        if (listResource.status == Status.ERROR){
            Snackbar.make(binding.constraintLayout, listResource.message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void setupViews() {
        setupRecyclerView();

        this.binding.createFab.setOnClickListener(v ->
                startActivity(new Intent(NotesFragment.this.getContext(), CreateActivity.class)));
    }

    private void setupRecyclerView() {
        //ToDO grid/linear changes no of columns depends on orientation and size
        this.binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_board, container, false);

        return binding.getRoot();
    }

    @Override
    public void onDeleteNote(Long id) {
        viewModel.deleteNote(id).observe(this, this::processDeleteResponse);
    }

    @Override
    public void onClickNote(Long id) {
        startActivity(new Intent(getContext(), NoteActivity.class));
    }

    private void processDeleteResponse(Resource<Boolean> booleanResource) {
        //TODO positon of RV should be saved
        boolean loading = booleanResource.status == Status.LOADING;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
