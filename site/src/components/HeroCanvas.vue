<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { resolved } from '../theme.js'

const canvas = ref(null)

// WebGL 材质不吃 CSS 变量，颜色按主题给两份（亮色下要压深一档才有对比度）
const COLORS = {
  dark: { knot: 0x41d1a5, particles: 0x9dc4ff },
  light: { knot: 0x0f9b76, particles: 0x2f6fd0 },
}

function applyColors() {
  const c = COLORS[resolved.value] || COLORS.dark
  if (knot) knot.material.color.setHex(c.knot)
  if (particles) particles.material.color.setHex(c.particles)
}

let renderer, scene, camera, knot, particles, rafId
const mouse = { x: 0, y: 0, tx: 0, ty: 0 }
let width = 0
let height = 0

function init() {
  const el = canvas.value
  if (!el) return
  const parent = el.parentElement
  width = parent.clientWidth
  height = parent.clientHeight

  renderer = new THREE.WebGLRenderer({
    canvas: el,
    antialias: true,
    alpha: true,
    powerPreference: 'low-power',
  })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(width, height, false)
  renderer.setClearColor(0x000000, 0)

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100)
  camera.position.set(0, 0, 7)

  // 中心线框二十面体
  const knotGeo = new THREE.IcosahedronGeometry(1.7, 1)
  const knotMat = new THREE.LineBasicMaterial({
    color: COLORS[resolved.value]?.knot ?? COLORS.dark.knot,
    transparent: true,
    opacity: 0.55,
  })
  knot = new THREE.LineSegments(new THREE.WireframeGeometry(knotGeo), knotMat)
  scene.add(knot)

  // 周围粒子
  const count = 220
  const positions = new Float32Array(count * 3)
  for (let i = 0; i < count; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 12
    positions[i * 3 + 1] = (Math.random() - 0.5) * 7
    positions[i * 3 + 2] = (Math.random() - 0.5) * 8
  }
  const pGeo = new THREE.BufferGeometry()
  pGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const pMat = new THREE.PointsMaterial({
    color: COLORS[resolved.value]?.particles ?? COLORS.dark.particles,
    size: 0.035,
    sizeAttenuation: true,
    transparent: true,
    opacity: 0.85,
  })
  particles = new THREE.Points(pGeo, pMat)
  scene.add(particles)
}

function tick(t) {
  rafId = requestAnimationFrame(tick)
  if (!renderer) return
  // 鼠标视差缓动
  mouse.x += (mouse.tx - mouse.x) * 0.04
  mouse.y += (mouse.ty - mouse.y) * 0.04

  const time = t * 0.0004
  knot.rotation.x = time * 0.5
  knot.rotation.y = time * 0.8
  camera.position.x = mouse.x * 0.6
  camera.position.y = -mouse.y * 0.4
  camera.lookAt(0, 0, 0)

  particles.rotation.y = -time * 0.2
  particles.rotation.x = time * 0.1

  renderer.render(scene, camera)
}

function onResize() {
  if (!renderer) return
  const parent = canvas.value?.parentElement
  if (!parent) return
  width = parent.clientWidth
  height = parent.clientHeight
  renderer.setSize(width, height, false)
  camera.aspect = width / height
  camera.updateProjectionMatrix()
}

function onMove(e) {
  const parent = canvas.value?.parentElement
  if (!parent) return
  const rect = parent.getBoundingClientRect()
  mouse.tx = ((e.clientX - rect.left) / rect.width - 0.5) * 2
  mouse.ty = ((e.clientY - rect.top) / rect.height - 0.5) * 2
}

onMounted(() => {
  init()
  rafId = requestAnimationFrame(tick)
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMove, { passive: true })
})

watch(resolved, applyColors)

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMove)
  if (knot) {
    knot.geometry.dispose()
    knot.material.dispose()
  }
  if (particles) {
    particles.geometry.dispose()
    particles.material.dispose()
  }
  renderer?.dispose()
})
</script>

<template>
  <canvas ref="canvas" class="hero-canvas" aria-hidden="true"></canvas>
</template>

<style scoped>
.hero-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}
</style>