package com.gym.system.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.Map;
import java.util.Locale;

public final class QrCodeService {
    private QrCodeService() {
    }

    public static String memberCode(int memberId) {
        if (memberId < 1) {
            throw new IllegalArgumentException("Member ID must be positive.");
        }
        return String.format(Locale.ROOT, "MEM-%04d", memberId);
    }

    public static String receiptCode(int paymentId) {
        if (paymentId < 1) {
            throw new IllegalArgumentException("Payment ID must be positive.");
        }
        return String.format(Locale.ROOT, "RCT-%06d", paymentId);
    }

    public static WritableImage createImage(String value, int size) throws WriterException {
        BitMatrix matrix = createMatrix(value, size);
        WritableImage image = new WritableImage(matrix.getWidth(), matrix.getHeight());
        PixelWriter pixels = image.getPixelWriter();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                pixels.setArgb(x, y, matrix.get(x, y) ? 0xFF111827 : 0xFFFFFFFF);
            }
        }
        return image;
    }

    public static BitMatrix createMatrix(String value, int size) throws WriterException {
        if (value == null || value.isBlank() || size < 64) {
            throw new IllegalArgumentException("A QR value and an image size of at least 64 are required.");
        }
        return new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
    }
}
