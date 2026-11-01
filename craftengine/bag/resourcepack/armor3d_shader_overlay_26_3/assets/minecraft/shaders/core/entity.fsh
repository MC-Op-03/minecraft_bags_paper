#version 330
#extension GL_ARB_separate_shader_objects : require

#define ARMOR3D_LAYER_FLAG_EMISSIVE 1
#define ARMOR3D_LAYER_FLAG_TINTED 2

#ifdef GLINT
#include <minecraft:globals.glsl>
#endif
#include <minecraft:oit.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
#ifdef DISSOLVE
uniform sampler2D DissolveMaskSampler;
#endif
#ifdef GLINT
uniform sampler2D GlintSampler;
#endif

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
layout(location = 2) in vec4 vertexPerFaceColorBack;
layout(location = 3) in vec4 vertexPerFaceColorFront;
#else
layout(location = 2) in vec4 vertexColor;
#endif
layout(location = 4) in vec4 lightColor;
layout(location = 5) in vec4 overlayColor;
layout(location = 6) in vec2 texCoord;
#ifdef GLINT
layout(location = 7) in vec2 texCoordGlint;
#endif
layout(location = 8) in vec2 armor3dNextTexCoord;
layout(location = 9) flat in float armor3dFrameBlend;
layout(location = 10) flat in int armor3dActive;
layout(location = 11) noperspective in float armor3dGeometryInvalid;
layout(location = 12) flat in int armor3dFullbright;
layout(location = 13) flat in vec4 armor3dTint;
layout(location = 14) flat in int armor3dLayerFlags;
layout(location = 15) flat in float armor3dEmissiveMaskOffsetV;
layout(location = 16) flat in float armor3dTintMaskOffsetV;
layout(location = 17) flat in vec4 armor3dAnimationMasks;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

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

#ifndef OIT_ALPHA_ONLY
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

#endif

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
#ifdef OIT_ADDITIVE
    textureColor.a = min(0.99, textureColor.a);
#endif
#ifdef ALPHA_CUTOUT
    if (armor3dActive == 0 && textureColor.a < ALPHA_CUTOUT) {
        discard;
    }
#endif
    vec4 finalColor = textureColor;

    if (armor3dActive == 1) {
        bool animated = (armor3dLayerFlags & 4) != 0;
        vec2 masks = animated ? armor3dAnimationMasks.xy : vec2(
            armor3dEmissiveMaskOffsetV > 0.0 ? armor3dEmissiveMaskOffsetV : -2.0,
            armor3dTintMaskOffsetV > 0.0 ? armor3dTintMaskOffsetV : -2.0);
        finalColor = armor3dShadeFrame(textureColor, texCoord, masks);
        if (animated && armor3dFrameBlend > 0.0) {
            vec4 nextColor = texture(Sampler0, armor3dNextTexCoord);
#ifdef OIT_ADDITIVE
            nextColor.a = min(0.99, nextColor.a);
#endif
            nextColor = armor3dShadeFrame(nextColor, armor3dNextTexCoord, armor3dAnimationMasks.zw);
            float alpha = mix(finalColor.a, nextColor.a, armor3dFrameBlend);
            vec3 premultiplied = mix(finalColor.rgb * finalColor.a,
                nextColor.rgb * nextColor.a, armor3dFrameBlend);
            finalColor = vec4(alpha > 0.0 ? premultiplied / alpha : vec3(0.0), alpha);
        }
    } else {
#ifdef PER_FACE_LIGHTING
        vec4 faceVertexColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
#else
        vec4 faceVertexColor = vertexColor;
#endif
#ifdef DISSOLVE
        if (faceVertexColor.a < texture(DissolveMaskSampler, texCoord).a) {
            discard;
        }
        faceVertexColor.a = 1.0;
#endif
        finalColor *= faceVertexColor * ColorModulator;
    }

#ifdef ALPHA_CUTOUT
    if (armor3dActive == 1 && finalColor.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

#ifdef GLINT
    finalColor.a = max(finalColor.a, GlintAlpha);
#endif

#ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, finalColor.a);
#else
#ifndef NO_OVERLAY
    finalColor.rgb = mix(overlayColor.rgb, finalColor.rgb, overlayColor.a);
#endif
#ifndef EMISSIVE
    if (armor3dActive == 0) {
        finalColor *= lightColor;
    }
#endif
#ifdef GLINT
    vec4 glintColor = GlintAlpha * texture(GlintSampler, texCoordGlint);
    finalColor.rgb += glintColor.rgb * glintColor.rgb;
#endif
#ifdef OIT_ACCUMULATE
    finalColor = sampleColorForAccumulation(finalColor);
    vec4 fogColor = vec4(FogColor.rgb * finalColor.a, FogColor.a);
#else
    vec4 fogColor = FogColor;
#endif
    fragColor = apply_fog(
        finalColor,
        sphericalVertexDistance,
        cylindricalVertexDistance,
        FogEnvironmentalStart,
        FogEnvironmentalEnd,
        FogRenderDistanceStart,
        FogRenderDistanceEnd,
        fogColor
    );
#endif
}
