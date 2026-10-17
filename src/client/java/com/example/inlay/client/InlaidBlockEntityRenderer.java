package com.example.inlay.client;

import com.example.inlay.block.InlaidBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Рисует куб «хоста». Если в грань вставлен другой блок - в грани делается углубление (рамка 3/16, глубина 2/16),
 * на дне которого лежит текстура вставленного блока, а стенки углубления - текстура хоста.
 */
public class InlaidBlockEntityRenderer implements BlockEntityRenderer<InlaidBlockEntity, InlaidRenderState> {
	/** Ширина рамки вокруг «окна» (в долях блока). Меняйте под вкус. */
	private static final float MARGIN = 3f / 16f;
	/** Насколько глубоко утоплен вставленный блок. */
	private static final float DEPTH = 2f / 16f;

	// Для каждой грани (порядок Direction: DOWN, UP, NORTH, SOUTH, WEST, EAST):
	// origin(3), uAxis(3), vAxis(3), normal(3). v направлена «вниз по текстуре».
	private static final float[][] BASIS = {
			/* DOWN  */ {0, 0, 1, 1, 0, 0, 0, 0, -1, 0, -1, 0},
			/* UP    */ {0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0},
			/* NORTH */ {1, 1, 0, -1, 0, 0, 0, -1, 0, 0, 0, -1},
			/* SOUTH */ {0, 1, 1, 1, 0, 0, 0, -1, 0, 0, 0, 1},
			/* WEST  */ {0, 1, 0, 0, 0, 1, 0, -1, 0, -1, 0, 0},
			/* EAST  */ {1, 1, 1, 0, 0, -1, 0, -1, 0, 1, 0, 0},
	};

	public InlaidBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
	}

	@Override
	public InlaidRenderState createRenderState() {
		return new InlaidRenderState();
	}

	@Override
	public void updateRenderState(InlaidBlockEntity be, InlaidRenderState state, float tickProgress, Vec3d cameraPos,
								  @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
		BlockEntityRenderer.super.updateRenderState(be, state, tickProgress, cameraPos, crumbling);

		BlockModels models = MinecraftClient.getInstance().getBlockRenderManager().getModels();
		state.hostSprite = models.getModelParticleSprite(be.getHost().getDefaultState());

		World world = be.getWorld();
		BlockPos pos = be.getPos();
		for (Direction d : Direction.values()) {
			int i = d.getIndex();
			Block inlay = be.getInlay(d);
			state.inlaySprites[i] = inlay == null ? null : models.getModelParticleSprite(inlay.getDefaultState());

			// Сам блок непрозрачный (свет внутри = 0), поэтому берём свет соседней клетки со стороны грани.
			if (world != null) {
				BlockPos np = pos.offset(d);
				state.faceLight[i] = LightmapTextureManager.pack(
						world.getLightLevel(LightType.BLOCK, np), world.getLightLevel(LightType.SKY, np));
			} else {
				state.faceLight[i] = LightmapTextureManager.MAX_LIGHT_COORDINATE;
			}
		}
	}

	@Override
	public void render(InlaidRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera) {
		if (state.hostSprite == null) return;

		queue.submitCustom(matrices, TexturedRenderLayers.entityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE),
				(entry, vc) -> {
					for (int i = 0; i < 6; i++) {
						drawFace(entry, vc, state.hostSprite, state.inlaySprites[i], BASIS[i], state.faceLight[i]);
					}
				});
	}

	private static void drawFace(MatrixStack.Entry e, VertexConsumer vc, Sprite host, @Nullable Sprite inlay,
								 float[] b, int light) {
		float[] n = {b[9], b[10], b[11]};
		float[] u = {b[3], b[4], b[5]};
		float[] v = {b[6], b[7], b[8]};
		float[] un = {-u[0], -u[1], -u[2]};
		float[] vn = {-v[0], -v[1], -v[2]};

		if (inlay == null) {
			rect(e, vc, host, light, 1f, n, b, 0, 0, 1, 1, 0, 0, 0, 1, 1);
			return;
		}

		float m = MARGIN, m1 = 1f - MARGIN, d = DEPTH;

		// Рамка на поверхности грани (текстура хоста, UV совпадает с положением - рамка «непрерывна»).
		rect(e, vc, host, light, 1f, n, b, 0, 0, 1, m, 0, 0, 0, 1, m);      // верх
		rect(e, vc, host, light, 1f, n, b, 0, m1, 1, 1, 0, 0, m1, 1, 1);    // низ
		rect(e, vc, host, light, 1f, n, b, 0, m, m, m1, 0, 0, m, m, m1);    // лево
		rect(e, vc, host, light, 1f, n, b, m1, m, 1, m1, 0, m1, m, 1, m1);  // право

		// Стенки углубления (темнее - имитация затенения).
		float wall = 0.62f;
		wall(e, vc, host, light, wall, u, b, m, m, m, m1, d, m, m1);      // левая стенка, смотрит вдоль +u
		wall(e, vc, host, light, wall, un, b, m1, m, m1, m1, d, m, m1);   // правая
		wall(e, vc, host, light, wall, v, b, m, m, m1, m, d, m, m1);      // верхняя
		wall(e, vc, host, light, wall, vn, b, m, m1, m1, m1, d, m, m1);   // нижняя

		// Дно: вставленный блок целиком (чуть темнее - он «в яме»).
		rect(e, vc, inlay, light, 0.82f, n, b, m, m, m1, m1, d, 0, 0, 1, 1);
	}

	/** Прямоугольник, параллельный грани, на глубине depth. (su0..sv1 - область спрайта в долях 0..1). */
	private static void rect(MatrixStack.Entry e, VertexConsumer vc, Sprite s, int light, float shade, float[] n,
							 float[] b, float u0, float v0, float u1, float v1, float depth,
							 float su0, float sv0, float su1, float sv1) {
		float[] p0 = pt(b, u0, v0, depth);
		float[] p1 = pt(b, u0, v1, depth);
		float[] p2 = pt(b, u1, v1, depth);
		float[] p3 = pt(b, u1, v0, depth);
		vert(e, vc, s, p0, su0, sv0, light, shade, n);
		vert(e, vc, s, p1, su0, sv1, light, shade, n);
		vert(e, vc, s, p2, su1, sv1, light, shade, n);
		vert(e, vc, s, p3, su1, sv0, light, shade, n);
	}

	/** Боковая стенка углубления: ребро (ua,va)->(ub,vb) на поверхности, уходящее на глубину d. */
	private static void wall(MatrixStack.Entry e, VertexConsumer vc, Sprite s, int light, float shade, float[] n,
							 float[] b, float ua, float va, float ub, float vb, float d, float along0, float along1) {
		float[] p0 = pt(b, ua, va, 0);
		float[] p1 = pt(b, ub, vb, 0);
		float[] p2 = pt(b, ub, vb, d);
		float[] p3 = pt(b, ua, va, d);
		vert(e, vc, s, p0, along0, 0, light, shade, n);
		vert(e, vc, s, p1, along1, 0, light, shade, n);
		vert(e, vc, s, p2, along1, d, light, shade, n);
		vert(e, vc, s, p3, along0, d, light, shade, n);
	}

	private static float[] pt(float[] b, float u, float v, float depth) {
		return new float[]{
				b[0] + b[3] * u + b[6] * v - b[9] * depth,
				b[1] + b[4] * u + b[7] * v - b[10] * depth,
				b[2] + b[5] * u + b[8] * v - b[11] * depth
		};
	}

	private static void vert(MatrixStack.Entry e, VertexConsumer vc, Sprite s, float[] p, float fu, float fv,
							 int light, float shade, float[] n) {
		float tu = s.getMinU() + (s.getMaxU() - s.getMinU()) * fu;
		float tv = s.getMinV() + (s.getMaxV() - s.getMinV()) * fv;
		int c = Math.round(255f * shade);
		vc.vertex(e, p[0], p[1], p[2])
				.color(c, c, c, 255)
				.texture(tu, tv)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(e, n[0], n[1], n[2]);
	}
}
