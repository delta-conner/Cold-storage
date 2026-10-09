<template>
  <div class="page-card placeholder-box">
    <h3>{{ $route.meta.title }}</h3>
    <p>{{ $route.meta.tip || '功能占位' }}</p>
  </div>
</template>

<script>
export default { name: 'Placeholder' }
</script>
