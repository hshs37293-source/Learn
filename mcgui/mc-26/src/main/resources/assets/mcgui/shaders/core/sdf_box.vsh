#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// Układ atrybutów = SdfPipeline.FORMAT (Position, UV0, Color, UV1, UV2, LineWidth).
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;        // 0..1 w obrębie quada
layout(location = 2) in vec4 Color;      // kolor narożnika
layout(location = 3) in ivec2 UV1;       // rozmiar quada, jednostka 0.25 px
layout(location = 4) in ivec2 UV2;       // promienie rogów: x = tl | tr<<7, y = br | bl<<7, jednostka 0.5 px
layout(location = 5) in float LineWidth; // >0 obrys (px), <0 cień (-promień rozmycia px), 0 wypełnienie

layout(location = 0) out vec2 vUv;
layout(location = 1) out vec4 vColor;
layout(location = 2) flat out ivec2 vSize;
layout(location = 3) flat out ivec2 vRadii;
layout(location = 4) out float vLine;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vUv = UV0;
    vColor = Color;
    vSize = UV1;
    vRadii = UV2;
    vLine = LineWidth;
}
