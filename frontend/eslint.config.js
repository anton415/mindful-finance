import path from 'node:path'
import { fileURLToPath } from 'node:url'
import js from '@eslint/js'
import eslintConfigPrettier from 'eslint-config-prettier/flat'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const eslintProfile =
  process.env.ESLINT_PROFILE === 'strict' ? 'strict' : 'safe'
const isStrictProfile = eslintProfile === 'strict'

const typeAwareConfig = isStrictProfile
  ? tseslint.configs.strictTypeChecked
  : tseslint.configs.recommendedTypeChecked

const codeShapeSeverity = isStrictProfile ? 'error' : 'warn'
const maxFileLines = isStrictProfile ? 300 : 400
const maxFunctionLines = isStrictProfile ? 80 : 120
const maxComplexity = isStrictProfile ? 10 : 15

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      typeAwareConfig,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: currentDir,
      },
    },
    rules: {
      '@typescript-eslint/no-misused-promises': [
        'error',
        {
          checksVoidReturn: {
            attributes: false,
          },
        },
      ],
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          args: 'after-used',
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          caughtErrors: 'all',
          caughtErrorsIgnorePattern: '^_',
          ignoreRestSiblings: true,
        },
      ],
      complexity: [codeShapeSeverity, maxComplexity],
      'max-lines': [
        codeShapeSeverity,
        {
          max: maxFileLines,
          skipBlankLines: true,
          skipComments: true,
        },
      ],
      'max-lines-per-function': [
        codeShapeSeverity,
        {
          max: maxFunctionLines,
          skipBlankLines: true,
          skipComments: true,
          IIFEs: true,
        },
      ],
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: [
                './features/*/*',
                '../features/*/*',
                '../../features/*/*',
                '../../../features/*/*',
              ],
              message: 'Import features through their public entrypoint.',
            },
          ],
        },
      ],
    },
  },
  eslintConfigPrettier,
])
