package com.example.datingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;

import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        EditText editLocation = view.findViewById(R.id.edit_location);
        EditText editGender = view.findViewById(R.id.edit_gender);
        Button btnFind = view.findViewById(R.id.btnFind);
        Log.d("frangment", "homee");

        // --- ⭐ Xử lý khi click Tìm tình iuu (GỬI DỮ LIỆU) ⭐ ---
        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lấy giá trị đã chọn từ EditText
                String selectedLocation = editLocation.getText().toString().trim();
                String selectedGender = editGender.getText().toString().trim();

                // Tạo Intent để chuyển sang DiscoveryActivity
                Intent i = new Intent(getActivity(), DiscoveryActivity.class);

                // Đính kèm dữ liệu vào Intent bằng putExtra
                i.putExtra("selectedLocation", selectedLocation);
                i.putExtra("selectedGender", selectedGender);

                startActivity(i);
            }
        });

        // Xử lý chọn tỉnh/thành
        editLocation.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_province_search, null);
            SearchView searchView = dialogView.findViewById(R.id.searchView);
            ListView listView = dialogView.findViewById(R.id.listView);

            String[] provinces = getResources().getStringArray(R.array.provinces);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, provinces);
            listView.setAdapter(adapter);

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.getFilter().filter(newText);
                    return true;
                }
            });

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("Chọn tỉnh/thành")
                    .setView(dialogView)
                    .create();

            listView.setOnItemClickListener((parent, view1, position, id) -> {
                String selected = adapter.getItem(position);
                editLocation.setText(selected);
                dialog.dismiss();
            });

            dialog.show();
        });

        // Xử lý chọn giới tính
        editGender.setOnClickListener(v -> {
            String[] genders = {"Nam", "Nữ", "Mọi người"};

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Chọn giới tính");
            builder.setItems(genders, (dialog, which) -> {
                editGender.setText(genders[which]);
            });
            builder.show();
        });

        return view;
    }
}