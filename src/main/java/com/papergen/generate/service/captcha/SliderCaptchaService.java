package com.papergen.generate.service.captcha;

import cn.hutool.core.util.IdUtil;
import com.papergen.generate.constant.CaptchaConstant;
import com.papergen.generate.domain.vo.CaptchaVO;
import com.papergen.generate.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 滑块验证码：Java2D 生成背景图和拼图块，答案保存在内存中，默认 120 秒过期且一次性使用。
 * 校验时客户端传来滑块水平拖动距离，与答案差值在容差内即通过。
 */
@Service
public class SliderCaptchaService {

    /**
     * 验证码缓存，key 为 captchaId
     */
    private final Map<String, CaptchaItem> cache = new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    @Value("${papergen.captcha.expire-seconds:120}")
    private long expireSeconds;

    @Value("${papergen.captcha.tolerance:5}")
    private int tolerance;

    /**
     * 生成一张新验证码，缺口横坐标即正确答案
     */
    public CaptchaVO generate() {
        int maxX = CaptchaConstant.WIDTH - CaptchaConstant.PIECE_SIZE - CaptchaConstant.MARGIN;
        int answerX = CaptchaConstant.MIN_X + random.nextInt(maxX - CaptchaConstant.MIN_X);

        int maxY = CaptchaConstant.HEIGHT - CaptchaConstant.PIECE_SIZE - CaptchaConstant.MARGIN;
        int answerY = CaptchaConstant.MIN_Y + random.nextInt(maxY - CaptchaConstant.MIN_Y);

        BufferedImage background = drawBackground();
        BufferedImage holeImage = drawHole(background, answerX, answerY);
        BufferedImage pieceImage = cutPiece(background, answerX, answerY);

        String captchaId = IdUtil.fastSimpleUUID();
        evictExpired();
        cache.put(captchaId, new CaptchaItem(answerX, System.currentTimeMillis() + expireSeconds * 1000L));

        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setBackgroundImage(toDataUrl(holeImage));
        vo.setSliderImage(toDataUrl(pieceImage));
        vo.setWidth(CaptchaConstant.WIDTH);
        vo.setHeight(CaptchaConstant.HEIGHT);
        vo.setSliderWidth(CaptchaConstant.PIECE_SIZE);
        vo.setSliderHeight(CaptchaConstant.PIECE_SIZE);
        vo.setSliderY(answerY);
        return vo;
    }

    /**
     * 校验并消费验证码，无论成功失败都不能重复使用
     */
    public boolean verify(String captchaId, Integer sliderX) {
        if (captchaId == null || sliderX == null) {
            return false;
        }
        CaptchaItem item = cache.remove(captchaId);
        if (item == null || item.expireAt() < System.currentTimeMillis()) {
            return false;
        }
        return Math.abs(item.answerX() - sliderX) <= tolerance;
    }

    /**
     * 清理已过期条目，避免缓存无限增长
     */
    private void evictExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().expireAt() < now);
    }

    /**
     * 绘制随机渐变背景
     */
    private BufferedImage drawBackground() {
        BufferedImage image = new BufferedImage(CaptchaConstant.WIDTH, CaptchaConstant.HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, randomColor(),
                CaptchaConstant.WIDTH, CaptchaConstant.HEIGHT, randomColor()));
        g.fillRect(0, 0, CaptchaConstant.WIDTH, CaptchaConstant.HEIGHT);

        // 叠加半透明圆形和线条增加干扰
        for (int i = 0; i < 12; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 70));
            g.fillOval(random.nextInt(CaptchaConstant.WIDTH), random.nextInt(CaptchaConstant.HEIGHT),
                    20 + random.nextInt(80), 20 + random.nextInt(70));
        }

        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 8; i++) {
            g.setColor(new Color(255, 255, 255, 60));
            g.drawLine(random.nextInt(CaptchaConstant.WIDTH), random.nextInt(CaptchaConstant.HEIGHT),
                    random.nextInt(CaptchaConstant.WIDTH), random.nextInt(CaptchaConstant.HEIGHT));
        }
        g.dispose();
        return image;
    }

    /**
     * 在背景图上绘制缺口
     */
    private BufferedImage drawHole(BufferedImage background, int x, int y) {
        BufferedImage image = new BufferedImage(CaptchaConstant.WIDTH, CaptchaConstant.HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(background, 0, 0, null);

        RoundRectangle2D shape = new RoundRectangle2D.Float(
                x, y, CaptchaConstant.PIECE_SIZE, CaptchaConstant.PIECE_SIZE, 12, 12);
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(shape);
        g.setColor(new Color(255, 255, 255, 190));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(shape);
        g.dispose();
        return image;
    }

    /**
     * 抠出缺口位置的拼图块，保留透明背景
     */
    private BufferedImage cutPiece(BufferedImage background, int x, int y) {
        BufferedImage piece = new BufferedImage(CaptchaConstant.PIECE_SIZE, CaptchaConstant.PIECE_SIZE,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = piece.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D shape = new RoundRectangle2D.Float(
                0, 0, CaptchaConstant.PIECE_SIZE, CaptchaConstant.PIECE_SIZE, 12, 12);
        g.setClip(shape);
        g.drawImage(background.getSubimage(x, y, CaptchaConstant.PIECE_SIZE, CaptchaConstant.PIECE_SIZE),
                0, 0, null);
        g.setClip(null);
        g.setColor(new Color(255, 255, 255, 210));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(shape);
        g.dispose();
        return piece;
    }

    private Color randomColor() {
        return new Color(90 + random.nextInt(130), 90 + random.nextInt(130), 90 + random.nextInt(130));
    }

    /**
     * 输出 data:image/png;base64 格式，前端可直接作为 img src
     */
    private String toDataUrl(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new BusinessException("验证码生成失败，请重试");
        }
    }


}
