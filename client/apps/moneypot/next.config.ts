import path from 'node:path';
import type { NextConfig } from 'next';

// The app lives three folders below the workspace root and uses packages from outside its own folder.
const workspaceRoot = path.join(__dirname, '../../..');

const nextConfig: NextConfig = {
  // keep the repository free of generated agent files
  agentRules: false,
  turbopack: { root: workspaceRoot },
  outputFileTracingRoot: workspaceRoot,
};

export default nextConfig;
