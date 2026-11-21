/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"DM Sans"', 'Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
      colors: {
        ink: {
          DEFAULT: '#111111',
          muted: '#555555',
          subtle: '#767676',
        },
        surface: {
          DEFAULT: '#FFFFFF',
          soft: '#F7F7F7',
          border: '#E5E5E5',
        },
      },
    },
  },
  plugins: [],
};
