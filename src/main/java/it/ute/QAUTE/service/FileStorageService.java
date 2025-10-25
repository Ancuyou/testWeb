package it.ute.QAUTE.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FileStorageService {
    private static final String UPLOAD_DIR = "src/main/resources/static/images/avatars";

    private static final Set<String> ALLOWED_EXT = Set.of("png","jpg","jpeg","webp");
    @Autowired
    private Cloudinary cloudinary;

    public String storeFile(MultipartFile file,String oldAvatar, int accountID) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File upload trống.");
        }
        if (oldAvatar != null && oldAvatar.contains("cloudinary.com")) {
            deleteFile(oldAvatar);
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());

        String ext = extractExtension(original);
        if (ext == null || ext.isBlank()) {
            ext = guessExtByContentType(file.getContentType());
        }
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new RuntimeException("Định dạng ảnh không hợp lệ. Chỉ chấp nhận: " + ALLOWED_EXT);
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", "avatars/" + accountID,
                            "folder", "avatars",
                            "overwrite", true,
                            "resource_type", "image"
                    ));
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("Không thể upload ảnh lên Cloudinary!", e);
        }
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return null;
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            String[] parts = imageUrl.split("/upload/");
            if (parts.length == 2) {
                String publicIdWithExt = parts[1].substring(parts[1].indexOf("/") + 1);
                String publicId = publicIdWithExt.substring(0, publicIdWithExt.lastIndexOf("."));

                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa ảnh: " + e.getMessage());
        }
    }
    private String guessExtByContentType(String ct) {
        if (ct == null) return null;
        ct = ct.toLowerCase(Locale.ROOT);
        if (ct.equals("image/png")) return "png";
        if (ct.equals("image/jpeg")) return "jpg";
        if (ct.equals("image/jpg")) return "jpg";
        if (ct.equals("image/webp")) return "webp";
        return null;
    }
}
