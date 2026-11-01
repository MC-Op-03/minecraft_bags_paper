#version 150

#define ARMOR3D_LAYER_FLAG_EMISSIVE 1
#define ARMOR3D_LAYER_FLAG_TINTED 2

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif
in vec4 lightColor;
in vec4 overlayColor;
in vec2 texCoord;
in vec2 armor3dNextTexCoord;
flat in float armor3dFrameBlend;
flat in int armor3dActive;
noperspective in float armor3dGeometryInvalid;
flat in int armor3dFullbright;
flat in vec4 armor3dTint;
flat in int armor3dLayerFlags;
flat in float armor3dEmissiveMaskOffsetV;
flat in float armor3dTintMaskOffsetV;
flat in vec4 armor3dAnimationMasks;

out vec4 fragColor;

vec4 armor3dShadeFrame(vec4 color, vec2 uv, vec2 maskOffsets) {
    bool hasVisiblePixel = color.a > 0.0;
    bool isEmissive = hasVisiblePixel
        && (armor3dLayerFlags & ARMOR3D_LAYER_FLAG_EMISSIVE) != 0;
    bool isTinted = hasVisiblePixel
        && (armor3dLayerFlags & ARMOR3D_LAYER_FLAG_TINTED) != 0;
    bool needsEmissiveMask = hasVisiblePixel
        && !isEmissive
        && maskOffsets.x > -1.5;
    bool needsTintMask = hasVisiblePixel && !isTinted && maskOffsets.y > -1.5;

    if (
        needsEmissiveMask
        && needsTintMask
        && maskOffsets.x == maskOffsets.y
    ) {
        bool sharedMaskPixel = texture(
            Sampler0,
            vec2(uv.x, uv.y + maskOffsets.x)
        ).a > 0.0;
        isEmissive = sharedMaskPixel;
        isTinted = sharedMaskPixel;
    } else {
        if (needsEmissiveMask) {
            isEmissive = texture(
                Sampler0,
                vec2(uv.x, uv.y + maskOffsets.x)
            ).a > 0.0;
        }
        if (needsTintMask) {
            isTinted = texture(
                Sampler0,
                vec2(uv.x, uv.y + maskOffsets.y)
            ).a > 0.0;
        }
    }

    if (isTinted) {
        color.rgb *= armor3dTint.rgb;
        color.a = 1.0;
    }

    if (armor3dFullbright == 0) {
#ifdef PER_FACE_LIGHTING
        color *= vertexPerFaceColorFront;
#else
        color *= vertexColor;
#endif
        if (!isEmissive) {
            color *= lightColor;
        }
    }

    if (isEmissive) {
        color.a = 1.0;
    }
    return color;
}

void main() {
    if (armor3dActive == 1 && armor3dGeometryInvalid > 0.0) {
        discard;
    }
#ifndef ARMOR3D_RENDER_BACK_FACES
    if (armor3dActive == 1 && !gl_FrontFacing) {
        discard;
    }
#endif

    vec4 textureColor = armor3dFrameBlend > 0.0 && (armor3dLayerFlags & 4) == 0
        ? mix(
            texture(Sampler0, texCoord),
            texture(Sampler0, armor3dNextTexCoord),
            armor3dFrameBlend
        )
        : texture(Sampler0, texCoord);
    vec4 finalColor = textureColor;

    if (armor3dActive == 1) {
        bool animated = (armor3dLayerFlags & 4) != 0;
        vec2 masks = animated ? armor3dAnimationMasks.xy : vec2(
            armor3dEmissiveMaskOffsetV > 0.0 ? armor3dEmissiveMaskOffsetV : -2.0,
            armor3dTintMaskOffsetV > 0.0 ? armor3dTintMaskOffsetV : -2.0);
        finalColor = armor3dShadeFrame(textureColor, texCoord, masks);
        if (animated && armor3dFrameBlend > 0.0) {
            vec4 nextColor = texture(Sampler0, armor3dNextTexCoord);
            nextColor = armor3dShadeFrame(nextColor, armor3dNextTexCoord, armor3dAnimationMasks.zw);
            float alpha = mix(finalColor.a, nextColor.a, armor3dFrameBlend);
            vec3 premultiplied = mix(finalColor.rgb * finalColor.a,
                nextColor.rgb * nextColor.a, armor3dFrameBlend);
            finalColor = vec4(alpha > 0.0 ? premultiplied / alpha : vec3(0.0), alpha);
        }
    } else {
#ifndef EMISSIVE
        finalColor *= lightColor;
#endif
#ifdef PER_FACE_LIGHTING
        finalColor *= gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
#else
        finalColor *= vertexColor;
#endif
        finalColor *= ColorModulator;
    }

#ifdef ALPHA_CUTOUT
    if (finalColor.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

#ifndef NO_OVERLAY
    finalColor.rgb = mix(overlayColor.rgb, finalColor.rgb, overlayColor.a);
#endif

    fragColor = apply_fog(
        finalColor,
        sphericalVertexDistance,
        cylindricalVertexDistance,
        FogEnvironmentalStart,
        FogEnvironmentalEnd,
        FogRenderDistanceStart,
        FogRenderDistanceEnd,
        FogColor
    );
}
