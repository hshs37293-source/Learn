#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;        // 0..1 w obrębie quada
layout(location = 2) in vec4 Color;      // wypełnienie (góra)
layout(location = 3) in vec4 Color2;     // wypełnienie (dół) / kolor obrysu
layout(location = 4) in vec4 Params0;    // szerokość, wysokość, grubość obrysu, (zarezerwowane)
layout(location = 5) in vec4 Radii;      // 4 promienie rogów: prawy-górny, prawy-dolny, lewy-dolny, lewy-górny

layout(location = 0) out vec2 vUv;
layout(location = 1) out vec4 vColor;
layout(location = 2) out vec4 vColor2;
layout(location = 3) flat out vec4 vParams0;
layout(location = 4) flat out vec4 vRadii;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vUv = UV0; vColor = Color; vColor2 = Color2; vParams0 = Params0; vRadii = Radii;
}
