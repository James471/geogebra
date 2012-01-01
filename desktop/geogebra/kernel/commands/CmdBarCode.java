package geogebra.kernel.commands;

import geogebra.common.kernel.AbstractKernel;
import geogebra.common.kernel.CircularDefinitionException;
import geogebra.common.kernel.arithmetic.Command;
import geogebra.common.kernel.arithmetic.NumberValue;
import geogebra.common.kernel.arithmetic.TextValue;
import geogebra.common.kernel.commands.CommandProcessor;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.kernel.geos.GeoImage;
import geogebra.common.kernel.geos.GeoPoint2;
import geogebra.common.kernel.geos.GeoText;
import geogebra.common.main.MyError;
import geogebra.euclidian.EuclidianView;
import geogebra.main.Application;
import geogebra.util.BarcodeFactory;

import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.util.Locale;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.EAN8Reader;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * ToolImage
 */
class CmdBarCode extends CommandProcessor {
	/**
	 * Create new command processor
	 * 
	 * @param kernel
	 *            kernel
	 */
	public CmdBarCode(AbstractKernel kernel) {
		super(kernel);
	}

	public GeoElement[] process(Command c) throws MyError {
		int n = c.getArgumentNumber();
		boolean[] ok = new boolean[n];
		GeoElement[] arg;
		arg = resArgs(c);

		switch (n) {
		case 0:
			// decode barcode from active Graphics View
			BufferedImage image = ((EuclidianView) app.getEuclidianView())
					.getExportImage(1.0);
			return decode(image, c);

		case 1:
			
			if (!arg[0].isGeoText()) {
				if (!arg[0].isGeoImage()) {
					throw argErr(app, c.getName(), arg[0]);
				}
	
				image = geogebra.awt.BufferedImage
						.getAwtBufferedImage(((GeoImage) arg[0]).getFillImage());
	
				return decode(image, c);
			}
			
			// GeoText: fall through

		case 2:
		case 3:
		case 4:
		case 5:
			// defaults
			ErrorCorrectionLevel errorLevel = ErrorCorrectionLevel.H;
			BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE;
			int width = 100;
			int height = 100;
			String text;

			if (arg[0].isTextValue() && arg[0].isDefined()) {
				text = ((TextValue) arg[0]).toValueString();
			} else {
				throw argErr(app, c.getName(), arg[0]);
			}

			int i = 1;

			if (i < arg.length && !arg[i].isDefined()) {
				throw argErr(app, c.getName(), arg[i]);
			}

			boolean checksumNeeded = false;
			String formatText = null;
			if (i < arg.length && arg[i].isTextValue()) {
				TextValue format = (TextValue) arg[i];
				try {
					formatText = format.getText()
							.toValueString().toUpperCase(Locale.US);
					barcodeFormat = BarcodeFormat.valueOf(formatText);
					checksumNeeded = formatText.startsWith("EAN")|| formatText.startsWith("UPC");
					Application.debug(formatText+" "+checksumNeeded);
				} catch (Exception e) {
					// default already set
					// barcodeFormat = BarcodeFormat.QR_CODE;
				}
				i++;
			}

			if (i < arg.length && !arg[i].isDefined()) {
				throw argErr(app, c.getName(), arg[i]);
			}

			if (i < arg.length && arg[i].isTextValue()) {
				TextValue error = (TextValue) arg[i];
				String errorStr = error.getText().toValueString()
						.toUpperCase(Locale.US);
				if (errorStr.length() > 0) {
					switch (errorStr.charAt(0)) {
					case 'L':
						errorLevel = ErrorCorrectionLevel.L;
						break;
					case 'M':
						errorLevel = ErrorCorrectionLevel.M;
						break;
					case 'Q':
						errorLevel = ErrorCorrectionLevel.Q;
						break;
					default:
						// errorLevel = ErrorCorrectionLevel.H;
					}
				}
				i++;
			}

			if (i < arg.length && !arg[i].isDefined()) {
				throw argErr(app, c.getName(), arg[i]);
			}

			if (i < arg.length && arg[i].isNumberValue()) {
				width = (int) ((NumberValue) arg[i]).getDouble();
				i++;
			}

			if (i < arg.length && !arg[i].isDefined()) {
				throw argErr(app, c.getName(), arg[i]);
			}

			if (i < arg.length && arg[i].isNumberValue()) {
				height = (int) ((NumberValue) arg[i]).getDouble();
				i++;
			}

			if (i != n) {
				throw argErr(app, c.getName(), arg[i]);
			}

			MultiFormatWriter writer = new MultiFormatWriter();
			Hashtable hints = new Hashtable();
			hints.put(EncodeHintType.ERROR_CORRECTION, errorLevel);
			BitMatrix matrix;
			try {
				
				if (checksumNeeded) {
					if (formatText.equals("EAN_8")) {
						text = (text+"00000000").substring(0,8);
						text = BarcodeFactory.addStandardUPCEANChecksum(text);
					} else if (formatText.equals("EAN_13")) {
						text = (text+"0000000000008").substring(0,13);
						text = BarcodeFactory.addStandardUPCEANChecksum(text);
					} 
				}
				
				
				matrix = writer.encode(text, barcodeFormat, (int) width,
						(int) height, hints);
				image = MatrixToImageWriter.toBufferedImage(matrix);
			} catch (Exception e1) {
				e1.printStackTrace();
				// some errors are OK
				// BarCode["12345","EAN_13"]
				// java.lang.IllegalArgumentException: Requested contents should be 13 digits long, but got 5
				//at com.google.zxing.oned.EAN13Writer.encode(EAN13Writer.java:50)
				
				// some are not too helpful
				// BarCode["123456789123a","EAN_13"]
				// java.lang.NumberFormatException: For input string: "a"
				//at java.lang.NumberFormatException.forInputString(NumberFormatException.java:48)
				//at java.lang.Integer.parseInt(Integer.java:447)
				//at java.lang.Integer.parseInt(Integer.java:497)
				//at com.google.zxing.oned.EAN13Writer.encode(EAN13Writer.java:73)
				
				
				GeoText geoText = new GeoText(cons, e1.getLocalizedMessage());
				geoText.setLabel(c.getLabel());
				GeoElement[] ret = { geoText };
				return ret;
			}

			String fileName = ((Application) app).createImage(image, "barcode"
					+ text + ".png");

			GeoImage geoImage = new GeoImage(app.getKernel().getConstruction());
			geoImage.setImageFileName(fileName);
			geoImage.setTooltipMode(GeoElement.TOOLTIP_OFF);

			boolean oldState = cons.isSuppressLabelsActive();
			cons.setSuppressLabelCreation(true);
			GeoPoint2 corner = new GeoPoint2(cons, null, 0, 0, 1);
			cons.setSuppressLabelCreation(oldState);
			try {
				geoImage.setStartPoint(corner);
			} catch (CircularDefinitionException e) {
			}
			geoImage.setLabel(null);

			GeoElement[] ret2 = { geoImage };
			return ret2;

		default:
			throw argNumErr(app, c.getName(), n);
		}
	}

	/*
	 * http://www.morovia.com/education/utility/upc-ean.asp
	 * http://code.google.com/p/zxing/wiki/DeveloperNotes
	 */
	private GeoElement[] decode(BufferedImage image, Command c) {
		
		Result result;
		
		/*
		LuminanceSource ls = new BufferedImageLuminanceSource(image);
		try {
			result = new QRCodeReader().decode(new BinaryBitmap(
					new HybridBinarizer(ls)));
		} catch (Exception e) {
			e.printStackTrace();
			result = null;
			//GeoElement[] ret = {};
			//return ret;

		}

		try {
			Hashtable hints = new Hashtable();
			//result = new MultiFormatUPCEANReader(hints).decode(new BinaryBitmap(
			//		new HybridBinarizer(ls)));
		      LuminanceSource source;
		        source = new BufferedImageLuminanceSource(image);
		      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		      result = new EAN8Reader().decode(bitmap, hints);

		} catch (Exception e) {
			e.printStackTrace();
			result = null;

		}
		*/
		try {
			// 
			Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
			hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
			Reader reader = new MultiFormatReader();
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			result = reader.decode(bitmap, hints);
		} catch (Exception e) {
			e.printStackTrace();
			result = null;

		}
		
		if (result == null) {
			GeoElement[] ret = {};
			return ret;
		}

		GeoText text = new GeoText(cons, result.getText());
		text.setLabel(c.getLabel());
		GeoElement[] ret = { text };
		return ret;
	}
}
